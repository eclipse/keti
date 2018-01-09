Setting Up Your Eclipse Environment
================================================================================
* Install Spring Tool Suite (these instructions are for STS 3.7.0)
* Create a new workspace.
* Select Spring Tool Suite->Preferences->General->Editors->Text Editor
  * Check "Insert Spaces for Tab"
  * Check "Show Print Margin" and set it to 120
  * Check "Show Line Numbers"
  * Check "Show Whitespace Characters"
* Select Spring Tool Suite->Preferences->XML->XML Files->Editor
  * Check "Indent Using Spaces" set "Indentation Size" to 4
* Import the eclipse preferences file.
  * In eclipse select File->Import->General->Preferences
  * Select the eclipse_config/eclipse.epf file
* Import the ACS maven project.
* The following steps are necessary to work around an Eclipse bug that prevents the formatter settings from taking effect.
  * Select Spring Tool Suite->Preferences->Java->Code Style->Formatter
   * Select the "Eclipse" formatter then click OK
   * Open a java file from your projects
   * Right click then select Source->Format
   * Save the file.
  * Select Spring Tool Suite->Preferences->Java->Code Style->Formatter
   * Select the "Guardians" formatter then click OK
   * Open the same java file that you opened before
   * Right click then select Source->Format
   * Save the file.

