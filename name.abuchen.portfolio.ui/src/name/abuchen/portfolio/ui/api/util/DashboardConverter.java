package name.abuchen.portfolio.ui.api.util;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.ui.api.dto.ColumnDto;
import name.abuchen.portfolio.ui.api.dto.DashboardDto;
import name.abuchen.portfolio.ui.api.dto.WidgetDto;
import name.abuchen.portfolio.model.Dashboard;

/**
 * Utility class for converting Dashboard model objects to DTOs.
 */
public class DashboardConverter {
    
    /**
     * Convert a Dashboard model to DashboardDto.
     * 
     * @param dashboard The Dashboard model object
     * @return DashboardDto with serialized data
     */
    public static DashboardDto toDto(Dashboard dashboard) {
        if (dashboard == null) {
            return null;
        }
        
        DashboardDto dto = new DashboardDto();
        dto.setId(dashboard.getId());
        dto.setName(dashboard.getName());
        dto.setConfiguration(dashboard.getConfiguration());
        
        // Convert columns
        List<ColumnDto> columnDtos = new ArrayList<>();
        if (dashboard.getColumns() != null) {
            for (Dashboard.Column column : dashboard.getColumns()) {
                ColumnDto columnDto = toColumnDto(column);
                columnDtos.add(columnDto);
            }
        }
        dto.setColumns(columnDtos);
        
        return dto;
    }
    
    /**
     * Convert a Dashboard.Column model to ColumnDto.
     * 
     * @param column The Dashboard.Column model object
     * @return ColumnDto with serialized data
     */
    private static ColumnDto toColumnDto(Dashboard.Column column) {
        if (column == null) {
            return null;
        }
        
        ColumnDto dto = new ColumnDto();
        dto.setWeight(column.getWeight());
        
        // Convert widgets
        List<WidgetDto> widgetDtos = new ArrayList<>();
        if (column.getWidgets() != null) {
            for (Dashboard.Widget widget : column.getWidgets()) {
                WidgetDto widgetDto = toWidgetDto(widget);
                widgetDtos.add(widgetDto);
            }
        }
        dto.setWidgets(widgetDtos);
        
        return dto;
    }
    
    /**
     * Convert a Dashboard.Widget model to WidgetDto.
     * 
     * @param widget The Dashboard.Widget model object
     * @return WidgetDto with serialized data
     */
    private static WidgetDto toWidgetDto(Dashboard.Widget widget) {
        if (widget == null) {
            return null;
        }
        
        WidgetDto dto = new WidgetDto();
        dto.setType(widget.getType());
        dto.setLabel(widget.getLabel());
        dto.setConfiguration(widget.getConfiguration());
        
        return dto;
    }
    
    /**
     * Convert a list of Dashboard models to a list of DashboardDto objects.
     * 
     * @param dashboards List of Dashboard model objects
     * @return List of DashboardDto objects
     */
    public static List<DashboardDto> toDtoList(List<Dashboard> dashboards) {
        if (dashboards == null) {
            return new ArrayList<>();
        }
        
        List<DashboardDto> dtos = new ArrayList<>();
        for (Dashboard dashboard : dashboards) {
            DashboardDto dto = toDto(dashboard);
            if (dto != null) {
                dtos.add(dto);
            }
        }
        return dtos;
    }
}

