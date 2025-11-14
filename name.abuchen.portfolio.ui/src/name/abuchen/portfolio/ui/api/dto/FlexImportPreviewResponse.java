package name.abuchen.portfolio.ui.api.dto;

import java.util.List;

/**
 * DTO for Flex import preview response.
 */
public class FlexImportPreviewResponse
{
    private List<ExtractedEntryDto> entries;
    private List<String> extractionErrors;
    private int totalEntries;
    private int entriesWithErrors;
    private int entriesWithWarnings;
    private int entriesOk;

    public List<ExtractedEntryDto> getEntries()
    {
        return entries;
    }

    public void setEntries(List<ExtractedEntryDto> entries)
    {
        this.entries = entries;
    }

    public List<String> getExtractionErrors()
    {
        return extractionErrors;
    }

    public void setExtractionErrors(List<String> extractionErrors)
    {
        this.extractionErrors = extractionErrors;
    }

    public int getTotalEntries()
    {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries)
    {
        this.totalEntries = totalEntries;
    }

    public int getEntriesWithErrors()
    {
        return entriesWithErrors;
    }

    public void setEntriesWithErrors(int entriesWithErrors)
    {
        this.entriesWithErrors = entriesWithErrors;
    }

    public int getEntriesWithWarnings()
    {
        return entriesWithWarnings;
    }

    public void setEntriesWithWarnings(int entriesWithWarnings)
    {
        this.entriesWithWarnings = entriesWithWarnings;
    }

    public int getEntriesOk()
    {
        return entriesOk;
    }

    public void setEntriesOk(int entriesOk)
    {
        this.entriesOk = entriesOk;
    }
}

