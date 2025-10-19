package name.abuchen.portfolio.ui.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for portfolio file information.
 * Contains basic information about a loaded portfolio file.
 */
public class PortfolioFileInfo {
    
    private String id;
    private String name;
    private String baseCurrency;
    private String timezone;
    private int version;
    private Set<String> saveFlags;
    private LocalDateTime lastModified;
    private boolean encrypted;
    private int securitiesCount;
    private int accountsCount;
    private int portfoliosCount;
    private int transactionsCount;
    private boolean clientLoaded;
    private String clientInfo;
    private List<DashboardDto> dashboards;
    private List<ReportingPeriodDto> reportingPeriods;
    
    @JsonProperty("securityaccounts")
    private List<PortfolioDto> portfolios;
    
    private List<AccountDto> accounts;
    private List<SecurityDto> securities;
    private List<TaxonomyDto> taxonomies;
    private List<TransactionDto> transactions;
    
    // Constructors
    public PortfolioFileInfo() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBaseCurrency() {
        return baseCurrency;
    }
    
    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public Set<String> getSaveFlags() {
        return saveFlags;
    }
    
    public void setSaveFlags(Set<String> saveFlags) {
        this.saveFlags = saveFlags;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public boolean isEncrypted() {
        return encrypted;
    }
    
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
    
    public int getSecuritiesCount() {
        return securitiesCount;
    }
    
    public void setSecuritiesCount(int securitiesCount) {
        this.securitiesCount = securitiesCount;
    }
    
    public int getAccountsCount() {
        return accountsCount;
    }
    
    public void setAccountsCount(int accountsCount) {
        this.accountsCount = accountsCount;
    }
    
    public int getPortfoliosCount() {
        return portfoliosCount;
    }
    
    public void setPortfoliosCount(int portfoliosCount) {
        this.portfoliosCount = portfoliosCount;
    }
    
    public int getTransactionsCount() {
        return transactionsCount;
    }
    
    public void setTransactionsCount(int transactionsCount) {
        this.transactionsCount = transactionsCount;
    }
    
    public boolean isClientLoaded() {
        return clientLoaded;
    }
    
    public void setClientLoaded(boolean clientLoaded) {
        this.clientLoaded = clientLoaded;
    }
    
    public String getClientInfo() {
        return clientInfo;
    }
    
    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }
    
    public List<DashboardDto> getDashboards() {
        return dashboards;
    }
    
    public void setDashboards(List<DashboardDto> dashboards) {
        this.dashboards = dashboards;
    }
    
    public List<ReportingPeriodDto> getReportingPeriods() {
        return reportingPeriods;
    }
    
    public void setReportingPeriods(List<ReportingPeriodDto> reportingPeriods) {
        this.reportingPeriods = reportingPeriods;
    }
    
    @JsonProperty("securityaccounts")
    public List<PortfolioDto> getPortfolios() {
        return portfolios;
    }
    
    @JsonProperty("securityaccounts")
    public void setPortfolios(List<PortfolioDto> portfolios) {
        this.portfolios = portfolios;
    }
    
    public List<AccountDto> getAccounts() {
        return accounts;
    }
    
    public void setAccounts(List<AccountDto> accounts) {
        this.accounts = accounts;
    }
    
    public List<SecurityDto> getSecurities() {
        return securities;
    }
    
    public void setSecurities(List<SecurityDto> securities) {
        this.securities = securities;
    }
    
    public List<TaxonomyDto> getTaxonomies() {
        return taxonomies;
    }
    
    public void setTaxonomies(List<TaxonomyDto> taxonomies) {
        this.taxonomies = taxonomies;
    }
    
    public List<TransactionDto> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(List<TransactionDto> transactions) {
        this.transactions = transactions;
    }
}
