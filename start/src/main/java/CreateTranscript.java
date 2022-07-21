/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outputs the transcript of an audio file into a
 * newly created Google Document.
 */
public class CreateTranscript {
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  // Specify audio file name below.
  private static final String AUDIO_FILENAME = "audiofile.wav";
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String APPLICATION_NAME = "CreateTranscript";
  private static final List<String> SCOPES = Collections.singletonList(DocsScopes.DOCUMENTS);

  public static void main(String args[]) throws IOException, GeneralSecurityException {
    final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    Docs service = new Docs.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
        .setApplicationName(APPLICATION_NAME)
        .build();

    createTranscript(service);
  }

  /**
   * Creates an authorized Credential object.
   *
   * @param {NetHttpTransport} httpTransport The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws {IOException} If the credentials.json file cannot be found.
   */
  static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
    InputStream in = CreateTranscript.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Credentials file not found at: " + CREDENTIALS_FILE_PATH);
    }

    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();

    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  /**
   * Creates a Google Doc with the text transcription of an audio file.
   *
   * @param {Docs} service Docs authorization service to be able to use the Docs API.
   */
  private static void createTranscript(Docs service) throws IOException {
    String docId = createDocument(service);
    List<Request> insertRequests = getTranscript();
    insertText(service, docId, insertRequests);
    String url = "https://docs.google.com/document/d/" + docId + "/edit";
    System.out.println("Your transcript is now available at " + url);
  }

  /**
   * Creates a new Google Document. Once the document is created, returns its Document ID.
   *
   * @param {Docs} service Docs authorized service to be able to create a Doc.
   * @return {String} Returns the Document ID of the newly created Doc.
   */
  private static String createDocument(Docs service) throws IOException {
    // TODO: Create a new Google Document in the authorized account.
  }

  /**
   * Obtains the transcript of an audio file by calling the Google Speech-to-Text API.
   *
   * @return {List<Request>} A list of text to insert into a Google Doc.
   */
  private static List<Request> getTranscript() throws IOException {
    // TODO: Obtain and return an audio file's text transcripts.
  }

  /**
   * Helper function that inserts text into a Google Document.
   *
   * @param {Docs} service Docs authorized service to be able to write to an existing Doc.
   * @param {String} docId Google Doc ID of the Doc you'll be writing to.
   * @param {List<Request>} insertRequests List of insertRequests to be inserted into the Doc.
   */
  private static void insertText(Docs service, String docId, List<Request> insertRequests) throws IOException {
    // TODO: Insert a list of text requests into a Google Document.
  }
}
