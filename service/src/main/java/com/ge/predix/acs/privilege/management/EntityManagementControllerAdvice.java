package com.ge.predix.acs.privilege.management;

import org.json.simple.JSONObject;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice(basePackageClasses = { SubjectPrivilegeManagementController.class,
        ResourcePrivilegeManagementController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EntityManagementControllerAdvice {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<JSONObject> handleIncorrectParamType() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject() {{
                    put(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_ERROR,
                            HttpStatus.BAD_REQUEST.getReasonPhrase());
                    put(PrivilegeManagementUtility.INCORRECT_PARAMETER_TYPE_MESSAGE,
                            "Request Parameter " + PrivilegeManagementUtility.INHERITED_ATTRIBUTES_REQUEST_PARAMETER
                                    + " must be a boolean value");
                }});
    }
}