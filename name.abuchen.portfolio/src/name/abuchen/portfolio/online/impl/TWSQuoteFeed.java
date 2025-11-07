package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Quote feed provider for Interactive Brokers TWS (Trader Workstation) API.
 * 
 * This provider connects to a local TWS API server to retrieve live and
 * historical price data for securities identified by ISIN.
 * 
 * Configuration properties:
 * - TWS_SERVER_HOST: Server host (default: localhost)
 * - TWS_SERVER_PORT: Server port (default: 8000)
 */
public final class TWSQuoteFeed implements QuoteFeed
{
    public static final String ID = "TWS"; //$NON-NLS-1$
    
    private static final String DEFAULT_HOST = "localhost"; //$NON-NLS-1$
    private static final String DEFAULT_PORT = "8000"; //$NON-NLS-1$
    
    private String serverHost;
    private String serverPort;

    public TWSQuoteFeed()
    {
        // Initialize with environment variables or default values
        String envHost = System.getenv("TWS_SERVER_HOST"); //$NON-NLS-1$
        String envPort = System.getenv("TWS_SERVER_PORT"); //$NON-NLS-1$
        
        this.serverHost = (envHost != null && !envHost.isBlank()) ? envHost : DEFAULT_HOST;
        this.serverPort = (envPort != null && !envPort.isBlank()) ? envPort : DEFAULT_PORT;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Interactive Brokers TWS"; //$NON-NLS-1$
    }

    /**
     * Returns a unique grouping criterion for each security to enable parallel price updates.
     * TWS API server can handle multiple concurrent requests, so we use the security's ISIN
     * to ensure each security is processed in its own job group.
     */
    @Override
    public String getGroupingCriterion(Security security)
    {
        // Use ISIN as unique identifier to enable parallel processing
        // Fallback to UUID if ISIN is not available
        String isin = security.getIsin();
        return isin != null && !isin.isBlank() ? (ID + ":" + isin) : (ID + ":" + security.getUUID()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns a unique grouping criterion for latest quotes to enable parallel updates.
     */
    @Override
    public String getLatestGroupingCriterion(Security security)
    {
        return getGroupingCriterion(security);
    }

    /**
     * Sets the TWS server host. Can be configured via security properties.
     */
    public void setServerHost(String serverHost)
    {
        if (serverHost != null && !serverHost.isBlank())
            this.serverHost = serverHost;
    }

    /**
     * Sets the TWS server port. Can be configured via security properties.
     */
    public void setServerPort(String serverPort)
    {
        if (serverPort != null && !serverPort.isBlank())
            this.serverPort = serverPort;
    }

    /**
     * Retrieves server configuration from security properties, environment variables, or defaults.
     * Priority: 1) Security properties, 2) Environment variables, 3) Defaults
     */
    private void loadServerConfig(Security security)
    {
        // Load environment variables or defaults
        String envHost = System.getenv("TWS_SERVER_HOST"); //$NON-NLS-1$
        String envPort = System.getenv("TWS_SERVER_PORT"); //$NON-NLS-1$
        
        this.serverHost = (envHost != null && !envHost.isBlank()) ? envHost : DEFAULT_HOST;
        this.serverPort = (envPort != null && !envPort.isBlank()) ? envPort : DEFAULT_PORT;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        loadServerConfig(security);
        
        if (security.getIsin() == null || security.getIsin().isBlank())
        {
            PortfolioLog.error("Missing ISIN for security " + security.getName()); //$NON-NLS-1$
            return Optional.empty();
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            String currency = security.getCurrencyCode() != null ? security.getCurrencyCode() : "USD"; //$NON-NLS-1$
            
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess(serverHost, "/live-price")
                            .withScheme("http")
                            .withPort(Integer.parseInt(serverPort))
                            .addParameter("isin", security.getIsin())
                            .addParameter("currency", currency);

            String response = webaccess.get();
            JSONObject json = (JSONObject) JSONValue.parse(response);

            if (json != null && "success".equals(json.get("status"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                Object lastObj = json.get("last"); //$NON-NLS-1$
                if (lastObj != null)
                {
                    long lastPrice = asPrice(lastObj);
                    
                    if (lastPrice > 0L)
                    {
                        LatestSecurityPrice price = new LatestSecurityPrice();
                        price.setDate(LocalDate.now());
                        price.setValue(lastPrice);
                        return Optional.of(price);
                    }
                }
            }
            else if (json != null)
            {
                String message = (String) json.get("message"); //$NON-NLS-1$
                PortfolioLog.error("TWS API error: " + (message != null ? message : "Unknown error")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        catch (IOException e)
        {
            PortfolioLog.error(e);
        }

        return Optional.empty();
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        loadServerConfig(security);
        
        if (security.getIsin() == null || security.getIsin().isBlank())
            return QuoteFeedData.withError(
                            new IOException("Missing ISIN for security " + security.getName())); //$NON-NLS-1$

        LocalDate quoteStartDate = null;

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

        return getHistoricalQuotes(security, collectRawResponse, quoteStartDate);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        loadServerConfig(security);
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("unchecked")
    private QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
    {
        QuoteFeedData data = new QuoteFeedData();

        try
        {
            String currency = security.getCurrencyCode() != null ? security.getCurrencyCode() : "USD"; //$NON-NLS-1$
            
            // Calculate duration from start date or use default
            String duration = "1 Y"; //$NON-NLS-1$
            if (start != null)
            {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now());
                if (daysBetween <= 31)
                    duration = "1 M"; //$NON-NLS-1$
                else if (daysBetween <= 186)
                    duration = "6 M"; //$NON-NLS-1$
                else if (daysBetween <= 365)
                    duration = "1 Y"; //$NON-NLS-1$
                else if (daysBetween <= 730)
                    duration = "2 Y"; //$NON-NLS-1$
                else
                    duration = "5 Y"; //$NON-NLS-1$
            }

            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess(serverHost, "/historical-data-by-isin")
                            .withScheme("http")
                            .withPort(Integer.parseInt(serverPort))
                            .addParameter("isin", security.getIsin())
                            .addParameter("currency", currency)
                            .addParameter("duration", duration);

            String response = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response);

            JSONObject json = (JSONObject) JSONValue.parse(response);

            if (json != null && "success".equals(json.get("status"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                JSONArray dataArray = (JSONArray) json.get("data"); //$NON-NLS-1$
                
                if (dataArray != null)
                {
                    dataArray.forEach(e -> {
                        JSONObject dataPoint = (JSONObject) e;

                        try
                        {
                            LocalDate date = YahooHelper.fromISODate(String.valueOf(dataPoint.get("date"))); //$NON-NLS-1$
                            long open = asPrice(dataPoint.get("open")); //$NON-NLS-1$
                            long high = asPrice(dataPoint.get("high")); //$NON-NLS-1$
                            long low = asPrice(dataPoint.get("low")); //$NON-NLS-1$
                            long close = asPrice(dataPoint.get("close")); //$NON-NLS-1$
                            long volume = asNumber(dataPoint.get("volume")); //$NON-NLS-1$

                            // Only add prices from the requested start date onwards
                            if (date != null && close > 0L && (start == null || !date.isBefore(start)))
                            {
                                LatestSecurityPrice price = new LatestSecurityPrice();
                                price.setDate(date);
                                price.setValue(close);
                                price.setHigh(high);
                                price.setLow(low);
                                price.setVolume(volume);
                                data.addPrice(price);
                            }
                        }
                        catch (IllegalArgumentException ex)
                        {
                            data.addError(ex);
                        }
                    });
                }
            }
            else if (json != null)
            {
                String message = (String) json.get("message"); //$NON-NLS-1$
                data.addError(new IOException("TWS API error: " + (message != null ? message : "Unknown error"))); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    private long asPrice(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return Values.Quote.factorize(n.doubleValue());

        throw new IllegalArgumentException(number.getClass().toString());
    }

    private long asNumber(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return n.longValue();

        throw new IllegalArgumentException(number.getClass().toString());
    }
}

