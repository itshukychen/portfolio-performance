package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * DTO representing an extracted entry from a Flex report with its validation status.
 */
public class ExtractedEntryDto
{
    private String type;
    private LocalDateTime date;
    private Money amount;
    private Long shares;
    private String securityName;
    private String securityUUID;
    private String accountPrimaryName;
    private String accountPrimaryUUID;
    private String accountSecondaryName;
    private String accountSecondaryUUID;
    private String portfolioPrimaryName;
    private String portfolioPrimaryUUID;
    private String portfolioSecondaryName;
    private String portfolioSecondaryUUID;
    private Code maxStatus;
    private List<StatusDto> statuses;
    private boolean willBeImported;
    private String note;
    private String source;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public LocalDateTime getDate()
    {
        return date;
    }

    public void setDate(LocalDateTime date)
    {
        this.date = date;
    }

    public Money getAmount()
    {
        return amount;
    }

    public void setAmount(Money amount)
    {
        this.amount = amount;
    }

    public Long getShares()
    {
        return shares;
    }

    public void setShares(Long shares)
    {
        this.shares = shares;
    }

    public String getSecurityName()
    {
        return securityName;
    }

    public void setSecurityName(String securityName)
    {
        this.securityName = securityName;
    }

    public String getSecurityUUID()
    {
        return securityUUID;
    }

    public void setSecurityUUID(String securityUUID)
    {
        this.securityUUID = securityUUID;
    }

    public String getAccountPrimaryName()
    {
        return accountPrimaryName;
    }

    public void setAccountPrimaryName(String accountPrimaryName)
    {
        this.accountPrimaryName = accountPrimaryName;
    }

    public String getAccountPrimaryUUID()
    {
        return accountPrimaryUUID;
    }

    public void setAccountPrimaryUUID(String accountPrimaryUUID)
    {
        this.accountPrimaryUUID = accountPrimaryUUID;
    }

    public String getAccountSecondaryName()
    {
        return accountSecondaryName;
    }

    public void setAccountSecondaryName(String accountSecondaryName)
    {
        this.accountSecondaryName = accountSecondaryName;
    }

    public String getAccountSecondaryUUID()
    {
        return accountSecondaryUUID;
    }

    public void setAccountSecondaryUUID(String accountSecondaryUUID)
    {
        this.accountSecondaryUUID = accountSecondaryUUID;
    }

    public String getPortfolioPrimaryName()
    {
        return portfolioPrimaryName;
    }

    public void setPortfolioPrimaryName(String portfolioPrimaryName)
    {
        this.portfolioPrimaryName = portfolioPrimaryName;
    }

    public String getPortfolioPrimaryUUID()
    {
        return portfolioPrimaryUUID;
    }

    public void setPortfolioPrimaryUUID(String portfolioPrimaryUUID)
    {
        this.portfolioPrimaryUUID = portfolioPrimaryUUID;
    }

    public String getPortfolioSecondaryName()
    {
        return portfolioSecondaryName;
    }

    public void setPortfolioSecondaryName(String portfolioSecondaryName)
    {
        this.portfolioSecondaryName = portfolioSecondaryName;
    }

    public String getPortfolioSecondaryUUID()
    {
        return portfolioSecondaryUUID;
    }

    public void setPortfolioSecondaryUUID(String portfolioSecondaryUUID)
    {
        this.portfolioSecondaryUUID = portfolioSecondaryUUID;
    }

    public Code getMaxStatus()
    {
        return maxStatus;
    }

    public void setMaxStatus(Code maxStatus)
    {
        this.maxStatus = maxStatus;
    }

    public List<StatusDto> getStatuses()
    {
        return statuses;
    }

    public void setStatuses(List<StatusDto> statuses)
    {
        this.statuses = statuses;
    }

    public boolean isWillBeImported()
    {
        return willBeImported;
    }

    public void setWillBeImported(boolean willBeImported)
    {
        this.willBeImported = willBeImported;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}

