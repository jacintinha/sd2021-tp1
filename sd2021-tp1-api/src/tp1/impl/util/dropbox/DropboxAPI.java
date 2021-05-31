package tp1.impl.util.dropbox;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.Spreadsheet;
import tp1.config.DropboxConfig;
import tp1.impl.util.dropbox.arguments.*;
import tp1.impl.util.dropbox.replies.ListFolderReturn;
import tp1.impl.util.dropbox.replies.ListFolderReturn.FolderEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;


public class DropboxAPI {
    private static final String apiKey = DropboxConfig.API_KEY;
    private static final String apiSecret = DropboxConfig.API_SECRET;
    private static final String accessTokenStr = DropboxConfig.ACCESS_TOKEN;

    protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    protected static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    private static final String DELETE_V2_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String DELETE_BATCH_V2_URL = "https://api.dropboxapi.com/2/files/delete_batch";
    private static final String CREATE_SPREADSHEET_V2_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String GET_SPREADSHEET_V2_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String LIST_FOLDER_URL = "https://api.dropboxapi.com/2/files/list_folder";
    private static final String LIST_FOLDER_CONTINUE_URL = "https://api.dropboxapi.com/2/files/list_folder/continue";


    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    private final Gson json;

    public DropboxAPI() {
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        accessToken = new OAuth2AccessToken(accessTokenStr);
        json = new Gson();
    }

    public boolean createDirectory(String directoryName) {
        OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
        createFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);

        createFolder.setPayload(json.toJson(new CreateFolderV2Args("/" + directoryName, false)));

        service.signRequest(accessToken, createFolder);

        Response r;

        try {
            r = service.execute(createFolder);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if (r.getCode() == 200 || r.getCode() == 409) {
            if (r.getCode() == 409) {
                System.out.println("Folder already existed.");
            }
            return true;
        } else {
            System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
            try {
                System.err.println(r.getBody());
            } catch (IOException e) {
                System.err.println("No body in the response");
            }
            return false;
        }
    }

    public boolean delete(String path) {
        OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_V2_URL);
        delete.addHeader("Content-Type", JSON_CONTENT_TYPE);

        delete.setPayload(json.toJson(new PathV2Args("/" + path)));

        service.signRequest(accessToken, delete);

        Response r = null;

        try {
            r = service.execute(delete);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if (r.getCode() == 200 || r.getCode() == 409) {
            return true;
        } else {
            System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
            try {
                System.err.println(r.getBody());
            } catch (IOException e) {
                System.err.println("No body in the response");
            }
            return false;
        }
    }

    public boolean deleteBatch(List<PathV2Args> entries) {
        OAuthRequest deleteBatch = new OAuthRequest(Verb.POST, DELETE_BATCH_V2_URL);
        deleteBatch.addHeader("Content-Type", JSON_CONTENT_TYPE);

        deleteBatch.setPayload(json.toJson(new EntryV2Args(entries)));

        service.signRequest(accessToken, deleteBatch);

        Response r = null;

        try {
            r = service.execute(deleteBatch);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if (r.getCode() == 200) {
            return true;
        } else {
            System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
            try {
                System.err.println(r.getBody());
            } catch (IOException e) {
                System.err.println("No body in the response");
            }
            return false;
        }
    }

    public boolean createFile(String path, Object sheet) {
        OAuthRequest createFile = new OAuthRequest(Verb.POST, CREATE_SPREADSHEET_V2_URL);
        createFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        createFile.addHeader("Dropbox-API-Arg", json.toJson(new CreateSpreadsheetV2Args("/" + path, "overwrite", false, false, false)));

        createFile.setPayload(json.toJson(sheet).getBytes(StandardCharsets.UTF_8));

        service.signRequest(accessToken, createFile);

        Response r = null;

        try {
            r = service.execute(createFile);
        } catch (Exception e) {
            System.out.println("HELLO TEAM");
            e.printStackTrace();
            return false;
        }

        //TODO Codes (print message?)
        if (r.getCode() == 200 || r.getCode() == 409) {
            return true;
        } else {
            System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
            try {
                System.err.println(r.getBody());
            } catch (IOException e) {
                System.err.println("No body in the response");
            }
            return false;
        }
    }

    public Spreadsheet getFile(String path) {
        OAuthRequest getFile = new OAuthRequest(Verb.POST, GET_SPREADSHEET_V2_URL);
        getFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        getFile.addHeader("Dropbox-API-Arg", json.toJson(new PathV2Args("/" + path)));

        service.signRequest(accessToken, getFile);

        Response r;

        try {
            r = service.execute(getFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //TODO Codes

        if (r.getCode() == 429) {
            System.out.println("429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429429");
        }

        if (r.getCode() == 200) {
            try {
                return this.json.fromJson(r.getBody(), Spreadsheet.class);
            } catch (IOException e) {
                System.out.println("No body in the response");
                return null;
            }
        } else {
            System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
            try {
                System.err.println(r.getBody());
            } catch (IOException e) {
                System.err.println("No body in the response");
            }
            return null;
        }
    }

    // rootDirectory has the form: domain-n/
    public List<PathV2Args> listFolder(String rootDirectory, String user) {
        List<PathV2Args> directoryContents = new LinkedList<>();

        OAuthRequest listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_URL);
        listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
        listDirectory.setPayload(json.toJson(new ListFolderArgs("/" + rootDirectory + user, false)));

        service.signRequest(accessToken, listDirectory);

        Response r = null;

        try {
            while (true) {
                r = service.execute(listDirectory);

                if (r.getCode() != 200) {
                    System.err.println("Failed to list directory. Status " + r.getCode() + ": " + r.getMessage());
                    System.err.println(r.getBody());
                    return null;
                }

                ListFolderReturn reply = json.fromJson(r.getBody(), ListFolderReturn.class);

                for (FolderEntry e : reply.getEntries()) {
                    directoryContents.add(new PathV2Args("/" + rootDirectory + e.toString()));
                }

                if (reply.has_more()) {
                    //There are more elements to read, prepare a new request (now a continuation)
                    listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_CONTINUE_URL);
                    listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
                    //In this case the arguments is just an object containing the cursor that was returned in the previous reply.
                    listDirectory.setPayload(json.toJson(new ListFolderContinueArgs(reply.getCursor())));
                    service.signRequest(accessToken, listDirectory);
                } else {
                    break; //There are no more elements to read. Operation can terminate.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return directoryContents;
    }
}
