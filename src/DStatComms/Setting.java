package DStatComms;

/**
 * Created by DK on 2017-05-15.
 */
public class Setting {
    private String Output;
    private String Value;


    //Constructors
    public Setting (){
        setOutput(null);
        setValue(null);
    }

    public Setting (String value, String output){
        setOutput(output);
        setValue(value);
    }
/*    public Setting (String value, String output, int max, int min){
        this.Output = setOutput(output);
        this.Value = setOutput(value);
    }*/

    public void setOutput(String output) {
        Output = output;
    }

    public void setValue(String value) {
        Value = value;
    }

    public String getOutput() {
        return Output;
    }

    public String getValue() {
        return Value;
    }

    @Override
    public String toString() {
        return String.format("Value: %s Output: %s",Value,Output);
    }
}
