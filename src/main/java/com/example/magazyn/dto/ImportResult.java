package com.example.magazyn.dto;

import java.util.List;

public class ImportResult {

    private int added;
    private int updated;
    private List<RowError> errors;

    public ImportResult() {}

    public ImportResult(int added, int updated, List<RowError> errors) {
        this.added = added;
        this.updated = updated;
        this.errors = errors;
    }

    public int getAdded() { return added; }
    public void setAdded(int added) { this.added = added; }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public List<RowError> getErrors() { return errors; }
    public void setErrors(List<RowError> errors) { this.errors = errors; }

    public static class RowError {
        private int row;
        private String message;

        public RowError() {}

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
