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
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
import com.google.api.services.docs.v1.model.Request;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
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
import org.apache.http.client.CredentialsProvider;

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
  * @param httpTransport The network HTTP Transport.
  * @return An authorized Credential object.
  * @throws IOException If the credentials.json file cannot be found.
  */
 static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
   InputStream in = CreateTranscript.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
   if (in == null) {
     throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
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
  * Calls helper functions to create Doc, get audio file's transcript, and insert transcript into
  * created Doc.
  *
  * @param {Object} service Docs authorization service to be able to use the Docs API.
  */
 private static void createTranscript(Docs service) throws IOException {
   // Calls helper functions.
   String docId = createDocument(service);
   List<Request> toInsert = getTranscript();
   insertText(service, docId, toInsert);
 }

 /**
  * Creates a new Google Document. Once the document is created, returns its Document ID.
  *
  * @param {Object} service Docs authorized service to be able to create a Doc.
  * @return {String} Returns the Document ID of the newly created Doc.
  */
 private static String createDocument(Docs service) throws IOException {
   Document doc = new Document().setTitle("Transcript for " + AUDIO_FILENAME);
   doc = service.documents().create(doc).execute();
   String documentId = doc.getDocumentId();
   return documentId;
 }

 /**
  * Obtains the transcript of an audio file by calling the Google Speech-to-Text API.
  *
  * @return {Object} A list of requests of the audio file's transcript.
  */
 private static List<Request> getTranscript() throws IOException {
   SpeechClient speech = SpeechClient.create();
   Path path = Paths.get(AUDIO_FILENAME);
   byte[] data = Files.readAllBytes(path);
   ByteString audioBytes = ByteString.copyFrom(data);

   // Configure request with local raw PCM audio.
   RecognitionConfig config =
       RecognitionConfig.newBuilder()
           .setEncoding(AudioEncoding.LINEAR16)
           .setLanguageCode("en-US")
           .setSampleRateHertz(8000)
           .build();
   RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
   RecognizeResponse response = speech.recognize(config, audio);
   List<SpeechRecognitionResult> results = response.getResultsList();

   // Initialize ArrayList to be filled with requests & returned.
   List<Request> requests = new ArrayList<>();

   for (SpeechRecognitionResult result : results) {
     // Using the first + most likely alternative transcript
     SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
     String toInsert = alternative.getTranscript();

     // Add requests array list to return.
     requests.add(
         new Request()
             .setInsertText(
                 new InsertTextRequest()
                     .setText(toInsert)
                     .setEndOfSegmentLocation(new EndOfSegmentLocation().setSegmentId(""))));
   }
   return requests;
 }

 /**
  * Helper function that inserts text into a Google Document.
  *
  * @param {Object} service Docs authorized service to be able to write to an existing Doc.
  * @param {Object} toInsert List of requests to be inserted into the Doc.
  * @param {String} docID Google Doc ID of the Doc you'll be writing to.
  */
 private static void insertText(Docs service, String docId, List<Request> toInsert) throws IOException {
   BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest();
   service.documents().batchUpdate(docId, body.setRequests(toInsert)).execute();
 }
}

