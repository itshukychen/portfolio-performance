package name.abuchen.portfolio.ui.api.dto;

import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;

/**
 * DTO representing a validation status message.
 */
public class StatusDto
{
    private Code code;
    private String message;

    public StatusDto()
    {
    }

    public StatusDto(Code code, String message)
    {
        this.code = code;
        this.message = message;
    }

    public Code getCode()
    {
        return code;
    }

    public void setCode(Code code)
    {
        this.code = code;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}

