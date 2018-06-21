package sq.rogue.rosettadrone;

public class MAVParameter {

    private String paramName;
    private Float paramValue;
    private short paramType;

    MAVParameter(String paramName, float paramValue, short paramType) {
        this.paramName = paramName;
        this.paramValue = paramValue;
        this.paramType = paramType;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public Float getParamValue() {
        return paramValue;
    }

    public void setParamValue(Float paramValue) {
        this.paramValue = paramValue;
    }

    public short getParamType() {
        return paramType;
    }

    public void setParamType(short paramType) {
        this.paramType = paramType;
    }


}
