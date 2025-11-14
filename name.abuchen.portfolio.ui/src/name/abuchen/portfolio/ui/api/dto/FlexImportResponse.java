package name.abuchen.portfolio.ui.api.dto;

import java.util.List;

/**
 * DTO for Flex import response.
 */
public class FlexImportResponse
{
    private boolean success;
    private List<String> errors;
    private int itemsImported;

    public FlexImportResponse()
    {
    }

    public FlexImportResponse(boolean success, List<String> errors, int itemsImported)
    {
        this.success = success;
        this.errors = errors;
        this.itemsImported = itemsImported;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public void setErrors(List<String> errors)
    {
        this.errors = errors;
    }

    public int getItemsImported()
    {
        return itemsImported;
    }

    public void setItemsImported(int itemsImported)
    {
        this.itemsImported = itemsImported;
    }
}

