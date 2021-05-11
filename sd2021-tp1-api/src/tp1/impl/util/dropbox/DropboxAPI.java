package tp1.impl.util.dropbox;
import java.io.IOException;
import java.util.Scanner;

import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;

import tp1.api.Spreadsheet;
import tp1.config.DropboxConfig;
import tp1.impl.util.dropbox.arguments.*;


public class DropboxAPI {
    private static final String apiKey = DropboxConfig.API_KEY;
    private static final String apiSecret = DropboxConfig.API_SECRET;
    private static final String accessTokenStr = DropboxConfig.ACCESS_TOKEN;

    protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    private static final String DELETE_V2_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String CREATE_SPREADSHEET_V2_URL = "https://content.dropboxapi.com/2/files/upload";

    private OAuth20Service service;
    private OAuth2AccessToken accessToken;

    private Gson json;

    public  DropboxAPI() {
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        accessToken = new OAuth2AccessToken(accessTokenStr);
        json = new Gson();
    }

    public boolean createDirectory( String directoryName ) {
        OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
        createFolder.addHeader("Content-Type", JSON_CONTENT_TYPE);

        createFolder.setPayload(json.toJson(new CreateFolderV2Args(directoryName, false)));

        service.signRequest(accessToken, createFolder);

        Response r = null;

        try {
            r = service.execute(createFolder);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if(r.getCode() == 200 || r.getCode() == 409) {
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

    public boolean delete(String directoryName) {
        OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_V2_URL);
        delete.addHeader("Content-Type", JSON_CONTENT_TYPE);

        delete.setPayload(json.toJson(new DeleteV2Args(directoryName)));

        service.signRequest(accessToken, delete);

        Response r = null;

        try {
            r = service.execute(delete);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if(r.getCode() == 200 || r.getCode() == 409) {
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

    public boolean createFile(String directoryName, Spreadsheet sheet) {
        OAuthRequest createFile = new OAuthRequest(Verb.POST, CREATE_SPREADSHEET_V2_URL);
        createFile.addHeader("Content-Type", JSON_CONTENT_TYPE);
        createFile.addHeader("Dropbox-API-Arg", json.toJson(new CreateSpreadsheetV2Args(directoryName+ "/"+ sheet.getSheetId(), "add", false, false, false)));
        createFile.setPayload(json.toJson(sheet));

        service.signRequest(accessToken, createFile);

        Response r = null;

        try {
            r = service.execute(createFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //TODO Codes (print message?)
        if(r.getCode() == 200 || r.getCode() == 409) {
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
}
