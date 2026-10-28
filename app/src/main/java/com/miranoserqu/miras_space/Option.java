package com.miranoserqu.miras_space;

public class Option<A> {

    protected final String location;
    protected final String nameLocation;
    protected final String descriptionLocation;
    protected final A defaultValue;
    protected A value;

    public Option(String location, String nameLocation, String descriptionLocation, A defaultValue){
        this.location = location;
        this.nameLocation = nameLocation;
        this.descriptionLocation = descriptionLocation;
        this.defaultValue = defaultValue;
        this.value = this.defaultValue;
    }

    public A getValue(){
        return this.value;
    }

    public void updateValue(A value){
        this.value = value;
    }

    public String getLocation() {
        return location;
    }

    public String getNameLocation() {
        return nameLocation;
    }

    public String getDescriptionLocation() {
        return descriptionLocation;
    }

    public A getDefaultValue() {
        return defaultValue;
    }

    public static class DoubleOption extends Option<Double> {

        private final double maxValue;
        private final double minValue;

        public DoubleOption(String location, String nameLocation, String descriptionLocation, double defaultValue, double maxValue, double minValue){
            super(location, nameLocation, descriptionLocation, defaultValue);
            this.maxValue = maxValue;
            this.minValue = minValue;
        }

        public DoubleOption(String location, String nameLocation, String descriptionLocation, double defaultValue){
            super(location, nameLocation, descriptionLocation, defaultValue);
            this.maxValue = Double.MAX_VALUE;
            this.minValue = Double.MIN_VALUE;
        }

        @Override
        public void updateValue(Double value){
            this.value = value <= this.minValue ? this.minValue : Math.min(value, maxValue);
        }

    }

    public static class IntegerOption extends Option<Integer> {

        private final int maxValue;
        private final int minValue;

        public IntegerOption(String location, String nameLocation, String descriptionLocation, int defaultValue, int maxValue, int minValue){
            super(location, nameLocation, descriptionLocation, defaultValue);
            this.maxValue = maxValue;
            this.minValue = minValue;
        }

        public IntegerOption(String location, String nameLocation, String descriptionLocation, int defaultValue){
            super(location, nameLocation, descriptionLocation, defaultValue);
            this.maxValue = Integer.MAX_VALUE;
            this.minValue = Integer.MIN_VALUE;
        }

        @Override
        public void updateValue(Integer value){
            this.value = value <= this.minValue ? this.minValue : Math.min(value, maxValue);
        }

    }

    public static class StringOption extends Option<String> {

        public StringOption(String location, String nameLocation, String descriptionLocation, String defaultValue){
            super(location, nameLocation, descriptionLocation, defaultValue);
        }

    }

    public static class ElectionOption<O extends DiscreteOption> extends Option<O> {

        public ElectionOption(String location, String nameLocation, String descriptionLocation, O defaultValue) {
            super(location, nameLocation, descriptionLocation, defaultValue);
        }

    }

    public interface DiscreteOption {

        String getNameLocation();
        String getLocation();

    }

    public enum WindowType implements DiscreteOption {
        FULL_SCREEN {
            @Override
            public String getNameLocation() {
                return "options:window_type:full_screen";
            }

            @Override
            public String getLocation() {
                return "full_screen";
            }
        },
        WINDOWED {
            @Override
            public String getNameLocation() {
                return "options:window_type:windowed";
            }

            @Override
            public String getLocation() {
                return "windowed_window";
            }
        }
    }

    public static final IntegerOption HEIGHT_RESOLUTION = new IntegerOption("height_resolution", "options:height_resolution_name", "options:height_resolution_description", 1080);
    public static final IntegerOption WEIGHT_RESOLUTION = new IntegerOption("weight_resolution", "options:weight_resolution_name", "options:weight_resolution_description", 1920);
    public static final ElectionOption<WindowType> WINDOW_TYPE = new ElectionOption<>("window_type", "options:window_type", "options:window_type_description", WindowType.WINDOWED);
    public static final DoubleOption ZOOM = new DoubleOption("zoom", "options:zoom", "options:zoom_description", 1, 3, 0.5);

}
