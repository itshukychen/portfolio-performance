package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Security search provider for Interactive Brokers TWS (Trader Workstation) API.
 * 
 * This provider connects to a local TWS API server to retrieve live price data
 * for securities identified by ISIN. It validates that a security can be found
 * in TWS and returns it as a search result.
 * 
 * Configuration:
 * - TWS_SERVER_HOST: Server host (default: localhost)
 * - TWS_SERVER_PORT: Server port (default: 8000)
 */
public class TWSSearchProvider implements SecuritySearchProvider
{
    static class Result implements ResultItem
    {
        private String isin;
        private String name;
        private String symbol;
        private String exchange;
        private String currency;

        public Result(String isin, String name, String symbol, String exchange, String currency)
        {
            this.isin = isin;
            this.name = name;
            this.symbol = symbol;
            this.exchange = exchange;
            this.currency = currency;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getType()
        {
            return "Stock"; //$NON-NLS-1$
        }

        @Override
        public String getExchange()
        {
            return exchange;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return null;
        }

        @Override
        public String getCurrencyCode()
        {
            return currency;
        }

        @Override
        public String getSource()
        {
            return NAME;
        }

        @Override
        public String getFeedId()
        {
            return TWSQuoteFeed.ID;
        }

        @Override
        public boolean hasPrices()
        {
            return true;
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, currency);
            security.setIsin(isin);
            security.setTickerSymbol(symbol);
            security.setFeed(TWSQuoteFeed.ID);
            return security;
        }
    }

    private static final String NAME = "Interactive Brokers TWS"; //$NON-NLS-1$
    private static final String DEFAULT_HOST = "localhost"; //$NON-NLS-1$
    private static final String DEFAULT_PORT = "8000"; //$NON-NLS-1$
    
    private String serverHost;
    private String serverPort;
    
    public TWSSearchProvider()
    {
        // Initialize with environment variables or default values
        String envHost = System.getenv("TWS_SERVER_HOST"); //$NON-NLS-1$
        String envPort = System.getenv("TWS_SERVER_PORT"); //$NON-NLS-1$
        
        this.serverHost = (envHost != null && !envHost.isBlank()) ? envHost : DEFAULT_HOST;
        this.serverPort = (envPort != null && !envPort.isBlank()) ? envPort : DEFAULT_PORT;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     * Sets the TWS server host.
     */
    public void setServerHost(String serverHost)
    {
        if (serverHost != null && !serverHost.isBlank())
            this.serverHost = serverHost;
    }

    /**
     * Sets the TWS server port.
     */
    public void setServerPort(String serverPort)
    {
        if (serverPort != null && !serverPort.isBlank())
            this.serverPort = serverPort;
    }

    /**
     * Searches for a security by ISIN using the TWS API.
     * 
     * The query should be an ISIN (International Securities Identification Number).
     * This method queries the TWS API to verify the ISIN exists and can provide
     * price data.
     * 
     * @param query ISIN to search for
     * @return List of found securities (typically 0 or 1 result)
     */
    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        if (query == null || query.isBlank())
            return Collections.emptyList();

        // Clean up query (remove whitespace)
        String isin = query.trim().toUpperCase();
        
        // Basic ISIN validation (should be 12 characters alphanumeric)
        if (isin.length() != 12 || !isin.matches("[A-Z]{2}[A-Z0-9]{10}")) //$NON-NLS-1$
            return Collections.emptyList();

        List<ResultItem> results = new ArrayList<>();

        try
        {
            // Query TWS API to get live price and verify the security exists
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess(serverHost, "/live-price")
                            .withScheme("http")
                            .withPort(Integer.parseInt(serverPort))
                            .addParameter("isin", isin);

            String response = webaccess.get();
            JSONObject json = (JSONObject) JSONValue.parse(response);

            if (json != null && "success".equals(json.get("status"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                // Extract contract information
                JSONObject contract = (JSONObject) json.get("contract"); //$NON-NLS-1$
                
                if (contract != null)
                {
                    String symbol = (String) contract.get("symbol"); //$NON-NLS-1$
                    String exchange = (String) contract.get("exchange"); //$NON-NLS-1$
                    String primaryExchange = (String) contract.get("primary_exchange"); //$NON-NLS-1$
                    String currency = (String) contract.get("currency"); //$NON-NLS-1$
                    String localSymbol = (String) contract.get("local_symbol"); //$NON-NLS-1$

                    // Use local_symbol as name if available, otherwise use symbol
                    String name = localSymbol != null ? localSymbol : symbol;
                    
                    // Use primary_exchange if available, otherwise use exchange
                    String displayExchange = primaryExchange != null ? primaryExchange : exchange;

                    Result result = new Result(isin, name, symbol, displayExchange, currency);
                    results.add(result);
                }
            }
            else if (json != null)
            {
                // Log error but don't throw - just return empty results
                String message = (String) json.get("message"); //$NON-NLS-1$
                PortfolioLog.info("TWS search: " + (message != null ? message : "Security not found")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        catch (Exception e)
        {
            // Log error and return empty results
            PortfolioLog.error("TWS search failed for ISIN " + isin + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            PortfolioLog.error(e);
        }

        return results;
    }
}

