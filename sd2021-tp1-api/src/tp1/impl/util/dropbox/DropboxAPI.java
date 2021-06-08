package tp1.impl.util.dropbox;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.Spreadsheet;
import tp1.config.DropboxConfig;
import tp1.impl.util.Mediator;
import tp1.impl.util.dropbox.arguments.*;
import tp1.impl.util.dropbox.replies.ListFolderReturn;
import tp1.impl.util.dropbox.replies.ListFolderReturn.FolderEntry;
import tp1.impl.util.encoding.JSON;

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

    public DropboxAPI() {
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        accessToken = new OAuth2AccessToken(accessTokenStr);
    }

    public boolean createDirectory(String directoryName) {
        OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
        createFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);

        createFolder.setPayload(JSON.encode(new CreateFolderV2Args("/" + directoryName, false)));

        service.signRequest(accessToken, createFolder);

        int retries = 0;
        while (retries < Mediator.MAX_RETRIES) {
            try {
                Response r = service.execute(createFolder);
                // 409 means folder already exists
                if (r.getCode() == 200 || r.getCode() == 409)
                    return true;
                if (r.getCode() == 429) {
                    retries++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retries++;
            }
        }
        return false;
    }

    public boolean delete(String path) {
        OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_V2_URL);
        delete.addHeader("Content-Type", JSON_CONTENT_TYPE);

        delete.setPayload(JSON.encode(new PathV2Args("/" + path)));

        service.signRequest(accessToken, delete);

        int retries = 0;
        while (retries < Mediator.MAX_RETRIES) {
            try {
                Response r = service.execute(delete);
                // 409 path not found
                if (r.getCode() == 200 || r.getCode() == 409)
                    return true;
                if (r.getCode() == 429) {
                    retries++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retries++;
            }
        }
        return false;
    }

    public boolean deleteBatch(List<PathV2Args> entries) {
        OAuthRequest deleteBatch = new OAuthRequest(Verb.POST, DELETE_BATCH_V2_URL);
        deleteBatch.addHeader("Content-Type", JSON_CONTENT_TYPE);

        deleteBatch.setPayload(JSON.encode(new EntryV2Args(entries)));

        service.signRequest(accessToken, deleteBatch);

        int retries = 0;
        while (retries < Mediator.MAX_RETRIES) {
            try {
                Response r = service.execute(deleteBatch);
                if (r.getCode() == 200)
                    return true;
                if (r.getCode() == 429) {
                    retries++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retries++;
            }
        }
        return false;
    }

    public boolean createFile(String path, Object sheet) {
        OAuthRequest createFile = new OAuthRequest(Verb.POST, CREATE_SPREADSHEET_V2_URL);
        createFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        createFile.addHeader("Dropbox-API-Arg", JSON.encode(new CreateSpreadsheetV2Args("/" + path, "overwrite", false, false, false)));

        createFile.setPayload(JSON.encode(sheet).getBytes(StandardCharsets.UTF_8));

        service.signRequest(accessToken, createFile);

        int retries = 0;
        while (retries < Mediator.MAX_RETRIES) {
            try {
                Response r = service.execute(createFile);
                if (r.getCode() == 200)
                    return true;
                if (r.getCode() == 429) {
                    retries++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retries++;
            }
        }
        return false;
    }

    public Spreadsheet getFile(String path) {
        OAuthRequest getFile = new OAuthRequest(Verb.POST, GET_SPREADSHEET_V2_URL);
        getFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        getFile.addHeader("Dropbox-API-Arg", JSON.encode(new PathV2Args("/" + path)));

        service.signRequest(accessToken, getFile);

        int retries = 0;
        while (retries < Mediator.MAX_RETRIES) {
            try {
                Response r = service.execute(getFile);
                if (r.getCode() == 200) {
                    try {
                        return JSON.decode(r.getBody(), Spreadsheet.class);
                    } catch (IOException e) {
                        return null;
                    }
                }
                if (r.getCode() == 429) {
                    retries++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retries++;
            }
        }
        return null;
    }

    // rootDirectory has the form: domain-n/
    public List<PathV2Args> listFolder(String rootDirectory, String user) {
        List<PathV2Args> directoryContents = new LinkedList<>();

        OAuthRequest listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_URL);
        listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
        listDirectory.setPayload(JSON.encode(new ListFolderArgs("/" + rootDirectory + user, false)));

        service.signRequest(accessToken, listDirectory);


        try {
            while (true) {
                Response r = service.execute(listDirectory);

                if (r.getCode() != 200) {
                    System.err.println("Failed to list directory. Status " + r.getCode() + ": " + r.getMessage());
                    System.err.println(r.getBody());
                    return null;
                }

                ListFolderReturn reply = JSON.decode(r.getBody(), ListFolderReturn.class);

                for (FolderEntry e : reply.getEntries()) {
                    directoryContents.add(new PathV2Args("/" + rootDirectory + e.toString()));
                }

                if (reply.has_more()) {
                    //There are more elements to read, prepare a new request (now a continuation)
                    listDirectory = new OAuthRequest(Verb.POST, LIST_FOLDER_CONTINUE_URL);
                    listDirectory.addHeader("Content-Type", JSON_CONTENT_TYPE);
                    //In this case the arguments is just an object containing the cursor that was returned in the previous reply.
                    listDirectory.setPayload(JSON.encode(new ListFolderContinueArgs(reply.getCursor())));
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
