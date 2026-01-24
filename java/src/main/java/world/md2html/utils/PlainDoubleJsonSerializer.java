package world.md2html.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.DecimalFormat;

public class PlainDoubleJsonSerializer extends JsonSerializer<Double> {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.################");

    static {
        FORMAT.setDecimalSeparatorAlwaysShown(false);
        FORMAT.setGroupingUsed(false);
    }

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeNumber(FORMAT.format(value));
    }
}
