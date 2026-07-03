package world.md2html.buildcache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.SortedSet;
import java.util.TreeSet;

@Data
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimaryDocumentInfo {

    @JsonProperty("output_file")
    String outputFile;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(using = SetToListSerializer.class)
    @JsonDeserialize(using = ListToSetDeserializer.class)
    @JsonProperty("derived_documents")
    @Builder.Default
    SortedSet<String> derivedDocuments = new TreeSet<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(using = SetToListSerializer.class)
    @JsonDeserialize(using = ListToSetDeserializer.class)
    @JsonProperty("dependencies")
    @Builder.Default
    SortedSet<String> dependencies = new TreeSet<>();
}
