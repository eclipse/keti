package com.ge.predix.cloudfoundry.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public final class CloudFoundryApplicationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryApplicationHelper.class);
    private static final int CF_SERVICE_INSTANCE_NAME_TAKEN = 60002;
    private static final int MAX_RETRIES = 10;

    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;

    private enum PublisherCallback {

        SUBSCRIBE {
            @Override
            String message(final String messageEnding) {
                return String.format("%s in progress: %s", MESSAGE_PREFIX, messageEnding);
            }
        },
        ERROR {
            @Override
            String message(final String messageEnding) {
                return String.format("%s error: %s", MESSAGE_PREFIX, messageEnding);
            }
        };

        private static final String MESSAGE_PREFIX = "Cloud Foundry operation";

        abstract String message(String messageEnding);
    }

    private static <T> Mono<T> addCommonCallbacks(final String messageEnding, final Mono<T> mono) {
        return mono.doOnSubscribe(onSubscribe -> LOGGER.info(PublisherCallback.SUBSCRIBE.message(messageEnding)))
                .doOnError(onError -> LOGGER.error(PublisherCallback.ERROR.message(messageEnding))).retry(MAX_RETRIES);
    }

    private static <T> Flux<T> addCommonCallbacks(final String messageEnding, final Flux<T> flux) {
        return flux.doOnSubscribe(onSubscribe -> LOGGER.info(PublisherCallback.SUBSCRIBE.message(messageEnding)))
                .doOnError(onError -> LOGGER.error(PublisherCallback.ERROR.message(messageEnding))).retry(MAX_RETRIES);
    }

    public void pushApplication(final String applicationName, final Path application,
            final Map<String, String> environmentVariables, final List<CloudFoundryService> services) {

        pushApplication(this.cloudFoundryOperations, applicationName, application).thenMany(
                bindServiceInstances(this.cloudFoundryOperations, applicationName,
                        services.stream().map(CloudFoundryService::getServiceInstanceName)
                                .collect(Collectors.toList())))
                .thenMany(setEnvironmentVariables(this.cloudFoundryOperations, applicationName, environmentVariables))
                .blockLast();
    }

    public void createServicesAndPushApplication(final String applicationName, final Path application,
            final Map<String, String> environmentVariables, final List<CloudFoundryService> services) {

        pushApplication(this.cloudFoundryOperations, applicationName, application)
                .thenMany(setEnvironmentVariables(this.cloudFoundryOperations, applicationName, environmentVariables))
                .thenMany(createServiceInstances(this.cloudFoundryOperations, services)).thenMany(
                bindServiceInstances(this.cloudFoundryOperations, applicationName,
                        services.stream().map(CloudFoundryService::getServiceInstanceName)
                                .collect(Collectors.toList()))).blockLast();
    }

    private static Mono<Void> pushApplication(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final Path application) {

        String messageEnding = String.format("push application '%s'", applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications()
                .push(PushApplicationRequest.builder().path(application).host(applicationName)
                        .domain(System.getenv("CF_BASE_DOMAIN")).buildpack("java-buildpack").diskQuota(2048)
                        .healthCheckType(ApplicationHealthCheck.PORT).memory(2048).name(applicationName).noStart(true)
                        .build()));
    }

    public void setEnvironmentVariables(final String applicationName, final Map<String, String> environmentVariables) {
        setEnvironmentVariables(this.cloudFoundryOperations, applicationName, environmentVariables).blockLast();
    }

    private static Flux<Void> setEnvironmentVariables(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final Map<String, String> environmentVariables) {

        return Flux.fromIterable(environmentVariables.entrySet()).concatMap(
                environmentVariable -> setEnvironmentVariable(cloudFoundryOperations, applicationName,
                        environmentVariable.getKey(), environmentVariable.getValue()));
    }

    private static Mono<Void> setEnvironmentVariable(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final String variableName, final String variableValue) {

        String messageEnding = String
                .format("set environment variable '%s': '%s' on '%s'", variableName, variableValue, applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications().setEnvironmentVariable(
                SetEnvironmentVariableApplicationRequest.builder().name(applicationName).variableName(variableName)
                        .variableValue(variableValue).build()));
    }

    public ApplicationEnvironments getApplicationEnvironments(final String applicationName) {

        String messageEnding = String.format("get environments for '%s'", applicationName);

        return addCommonCallbacks(messageEnding, this.cloudFoundryOperations.applications()
                .getEnvironments(GetApplicationEnvironmentsRequest.builder().name(applicationName).build())).block();
    }

    public void createServiceInstances(final List<CloudFoundryService> services) {
        createServiceInstances(this.cloudFoundryOperations, services).blockLast();
    }

    private static Flux<Void> createServiceInstances(final CloudFoundryOperations cloudFoundryOperations,
            final List<CloudFoundryService> services) {

        return Flux.fromIterable(services).concatMap(
                service -> createServiceInstance(cloudFoundryOperations, service.getPlanName(),
                        service.getServiceInstanceName(), service.getServiceName(), service.getParameters()));
    }

    private static Mono<Void> createServiceInstance(final CloudFoundryOperations cloudFoundryOperations,
            final String planName, final String serviceInstanceName, final String serviceName,
            final Map<String, Object> parameters) {

        CreateServiceInstanceRequest.Builder createServiceInstanceRequestBuilder = CreateServiceInstanceRequest
                .builder().planName(planName).serviceInstanceName(serviceInstanceName).serviceName(serviceName);

        if (MapUtils.isNotEmpty(parameters)) {
            createServiceInstanceRequestBuilder.parameters(parameters);
        }

        String messageEnding = String
                .format("create service instance '%s' with plan '%s', service name '%s' and" + " parameters '%s'",
                        serviceInstanceName, planName, serviceName, parameters);

        return addCommonCallbacks(messageEnding,
                cloudFoundryOperations.services().createInstance(createServiceInstanceRequestBuilder.build())
                        .onErrorResume(ExceptionUtils.statusCode(CF_SERVICE_INSTANCE_NAME_TAKEN),
                                fallback -> Mono.empty()));
    }

    public ServiceInstance getServiceInstance(final String serviceInstanceName) {

        String messageEnding = String.format("get service instance '%s'", serviceInstanceName);

        return addCommonCallbacks(messageEnding, this.cloudFoundryOperations.services()
                .getInstance(GetServiceInstanceRequest.builder().name(serviceInstanceName).build())).block();
    }

    public void bindServiceInstances(final String applicationName, final List<String> serviceInstanceNames) {
        bindServiceInstances(this.cloudFoundryOperations, applicationName, serviceInstanceNames).blockLast();
    }

    private static Flux<Void> bindServiceInstances(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final List<String> serviceInstanceNames) {

        return Flux.fromIterable(serviceInstanceNames).concatMap(
                serviceInstanceName -> bindServiceInstance(cloudFoundryOperations, applicationName,
                        serviceInstanceName));
    }

    private static Mono<Void> bindServiceInstance(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final String serviceInstanceName) {

        String messageEnding = String
                .format("bind service instance '%s' to '%s'", serviceInstanceName, applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.services()
                .bind(BindServiceInstanceRequest.builder().applicationName(applicationName)
                        .serviceInstanceName(serviceInstanceName).build()));
    }

    public void startApplication(final String applicationName) {
        startApplication(this.cloudFoundryOperations, applicationName).block();
    }

    private static Mono<Void> startApplication(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName) {

        String messageEnding = String.format("start application '%s'", applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications()
                .start(StartApplicationRequest.builder().name(applicationName).build()));
    }

    // TODO: Add CF logging to integration tests once proxy issues related to pushing/deleting applications are fixed
    // in the latest cf-java-client version
    private static Flux<LogMessage> logs(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName) {

        String messageEnding = String.format("get logs for application '%s'", applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications()
                .logs(LogsRequest.builder().name(applicationName).recent(false).build()));
    }

    public void unbindAndDeleteServicesAndApplication(final String applicationName,
            final List<String> serviceInstanceNames) {

        unbindServiceInstances(this.cloudFoundryOperations, applicationName, serviceInstanceNames)
                .thenMany(deleteServiceInstances(this.cloudFoundryOperations, serviceInstanceNames))
                .thenEmpty(deleteApplication(this.cloudFoundryOperations, applicationName)).block();
    }

    public void unbindServicesAndDeleteApplication(final String applicationName,
            final List<String> serviceInstanceNames) {

        unbindServiceInstances(this.cloudFoundryOperations, applicationName, serviceInstanceNames)
                .thenEmpty(deleteApplication(this.cloudFoundryOperations, applicationName)).block();
    }

    public void deleteApplication(final String applicationName) {
        deleteApplication(this.cloudFoundryOperations, applicationName).block();
    }

    private static Flux<Void> unbindServiceInstances(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final List<String> serviceInstanceNames) {

        return Flux.fromIterable(serviceInstanceNames).concatMap(
                serviceInstanceName -> unbindServiceInstance(cloudFoundryOperations, applicationName,
                        serviceInstanceName));
    }

    private static Mono<Void> unbindServiceInstance(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName, final String serviceInstanceName) {

        String messageEnding = String
                .format("unbind service instance '%s' from '%s'", serviceInstanceName, applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.services()
                .unbind(UnbindServiceInstanceRequest.builder().applicationName(applicationName)
                        .serviceInstanceName(serviceInstanceName).build()).onErrorResume(throwable ->
                        (throwable instanceof IllegalArgumentException && (throwable.getMessage()
                                .contains(String.format("Service instance %s does not exist", serviceInstanceName))
                                || throwable.getMessage()
                                .contains(String.format("Application %s does not exist", applicationName)))) || (
                                throwable instanceof IllegalStateException && throwable.getMessage().contains(
                                        String.format("Service instance %s is not bound to application",
                                                serviceInstanceName))), fallback -> Mono.empty()));
    }

    private static Flux<Void> deleteServiceInstances(final CloudFoundryOperations cloudFoundryOperations,
            final List<String> serviceInstanceNames) {

        return Flux.fromIterable(serviceInstanceNames)
                .concatMap(serviceInstanceName -> deleteServiceInstance(cloudFoundryOperations, serviceInstanceName));
    }

    private static Mono<Void> deleteServiceInstance(final CloudFoundryOperations cloudFoundryOperations,
            final String serviceInstanceName) {

        String messageEnding = String.format("delete service instance '%s'", serviceInstanceName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.services()
                .deleteInstance(DeleteServiceInstanceRequest.builder().name(serviceInstanceName).build()).onErrorResume(
                        throwable -> throwable instanceof IllegalArgumentException && throwable.getMessage()
                                .contains(String.format("Service instance %s does not exist", serviceInstanceName)),
                        fallback -> Mono.empty()));
    }

    private static Mono<Void> deleteApplication(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName) {

        String messageEnding = String.format("delete application '%s'", applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications()
                .delete(DeleteApplicationRequest.builder().name(applicationName).deleteRoutes(false).build())
                .onErrorResume(throwable -> throwable instanceof IllegalArgumentException && throwable.getMessage()
                                .contains(String.format("Application %s does not exist", applicationName)),
                        fallback -> Mono.empty()));
    }

    public boolean applicationStarted(final String applicationName) {
        String applicationState = getApplication(this.cloudFoundryOperations, applicationName).block()
                .getRequestedState();
        LOGGER.info("Requested state of application '{}': {}", applicationName, applicationState);
        return StringUtils.containsIgnoreCase(applicationState, "started");
    }

    private static Mono<ApplicationDetail> getApplication(final CloudFoundryOperations cloudFoundryOperations,
            final String applicationName) {

        String messageEnding = String.format("get application '%s'", applicationName);

        return addCommonCallbacks(messageEnding, cloudFoundryOperations.applications()
                .get(GetApplicationRequest.builder().name(applicationName).build()));
    }

    public List<LogMessage> getLogs(final String applicationName) {
        return this.cloudFoundryOperations.applications()
                .logs(LogsRequest.builder().name(applicationName).recent(true).build()).buffer().blockLast();
    }
}
