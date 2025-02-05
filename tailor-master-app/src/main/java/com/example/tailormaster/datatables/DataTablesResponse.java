package com.example.tailormaster.datatables;

import java.util.List;

public class DataTablesResponse {
    private int draw; // The draw counter from DataTables
    private long recordsTotal; // Total number of records in the database
    private long recordsFiltered; // Total number of records after filtering
    private List<?> data; // Data to be displayed on the current page

    // Getters and Setters
    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public long getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public long getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> data) {
        this.data = data;
    }
}
