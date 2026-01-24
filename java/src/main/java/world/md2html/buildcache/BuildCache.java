package world.md2html.buildcache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import world.md2html.utils.PlainDoubleJsonSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Data
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
public class BuildCache {
    @JsonProperty("arg_file_mtime")
    @JsonSerialize(using = PlainDoubleJsonSerializer.class)
    double argFileMtime;
    @Builder.Default
    @JsonProperty("primary_documents")
    Map<String, PrimaryDocumentInfo> primaryDocuments = new LinkedHashMap<>();
    @JsonSerialize(using = SetToPreservedListSerializer.class)
    @JsonDeserialize(using = ListToSetDeserializer.class)
    @Builder.Default
    @JsonProperty("standalone_derived_documents")
    SortedSet<String> standaloneDerivedDocuments = new TreeSet<>();
}
