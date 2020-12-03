package ca.baosiek.model;

import java.time.Duration;

public class ServicesModel {

    private Duration graceTimeDuration; // Grace time duration
    private TableModel tableModel;

    // Grace time flag, i.e., if grace time should be executed for the above duration
    private Boolean graceTimeFlag;

    public ServicesModel() {

        this.graceTimeFlag = Boolean.valueOf(false);
    }

    public void setGraceTime(Duration duration) {

        this.graceTimeDuration = duration;
    }

    public Duration getGraceTime() {

        return this.graceTimeDuration;
    }

    public void setGraceTimeFlag(Boolean graceTimeFlag) {

        this.graceTimeFlag = graceTimeFlag;
    }

    public Boolean getGraceTimeFlag() {

        return this.graceTimeFlag;
    }

    public void setTableModel(TableModel tableModel) {

        this.tableModel = tableModel;
    }

    public TableModel getTableModel() {

        return this.tableModel;
    }

}
