package com.example.board.support;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;
import org.springframework.restdocs.RestDocumentationContext;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.Snippet;

/**
 * REST Docs snippet 헬퍼. {@code .andDo(ApiDocs.snippet("posts-list-200"))} 형태로 호출하면
 * build/generated-snippets/posts-list-200/ 아래에 request-body.json / response-body.json 이
 * pretty-print 된 채로 저장된다. 표준 REST Docs 의 .adoc 출력은 AsciiDoc 래퍼를 갖기 때문에, 프론트가 그대로 import 할 수 있도록 raw
 * JSON 만 별도 snippet 으로 dump 한다.
 */
public final class ApiDocs {

  private ApiDocs() {}

  public static RestDocumentationResultHandler snippet(String identifier) {
    return document(
        identifier,
        preprocessRequest(prettyPrint()),
        preprocessResponse(prettyPrint()),
        new RawBodySnippet("request-body.json", op -> op.getRequest().getContentAsString()),
        new RawBodySnippet("response-body.json", op -> op.getResponse().getContentAsString()));
  }

  private static final class RawBodySnippet implements Snippet {

    private final String filename;
    private final Function<Operation, String> extractor;

    private RawBodySnippet(String filename, Function<Operation, String> extractor) {
      this.filename = filename;
      this.extractor = extractor;
    }

    @Override
    public void document(Operation operation) throws IOException {
      RestDocumentationContext context =
          (RestDocumentationContext)
              operation.getAttributes().get(RestDocumentationContext.class.getName());
      File outDir = new File(context.getOutputDirectory(), operation.getName());
      Files.createDirectories(outDir.toPath());
      String body = extractor.apply(operation);
      if (body == null) {
        body = "";
      }
      Files.writeString(new File(outDir, filename).toPath(), body);
    }
  }
}
