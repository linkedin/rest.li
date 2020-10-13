# Inspection Description

The following is taken from [this documentation](https://www.jetbrains.org/intellij/sdk/docs/tutorials/code_inspections.html#inspection-description):

> The inspection description is an HTML file. The description is displayed in the upper right panel of the Inspections
> Preferences dialog when an inspection is selected from the list.
>
> Implicit in using `LocalInspectionTool` in the class hierarchy of the inspection implementation means following some
> conventions.
>
> The inspection description file is expected to be located under `<plugin root dir>/resources/inspectionDescriptions/`.
> If the inspection description file is to be located elsewhere, override `getDescriptionUrl()` in the inspection
> implementation class. The name of the description file is expected to be the inspection `<short name>.html` as
> provided by the inspection description or the inspection implementation class. If a short name is not provided by the
> plugin, the IntelliJ Platform computes one.
