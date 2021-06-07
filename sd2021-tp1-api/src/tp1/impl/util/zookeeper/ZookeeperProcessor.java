package tp1.impl.util.zookeeper;

import org.apache.zookeeper.*;
import tp1.impl.server.rest.resources.SpreadsheetRep;

import java.util.Collections;
import java.util.List;

public class ZookeeperProcessor implements Watcher {

    private static ZookeeperProcessor instance;

    private String primaryURL;
    private final ZooKeeper zk;
    private final String domain;
    private final String serverURI;
    private final SpreadsheetRep resource;

    /**
     * @param hostPort the port to run at
     */
    public ZookeeperProcessor(String hostPort, String domain, String serverURI, SpreadsheetRep resource) throws Exception {
        this.zk = new ZooKeeper(hostPort, 3000, this);
        this.domain = domain;
        this.serverURI = serverURI;
        this.primaryURL = "";
        this.resource = resource;

        // Create PERSISTENT
        this.createPersistent("/" + this.domain);

        // Set watcher
        this.createWatcher("/" + this.domain);

        // Create EPHEMERAL
        this.createEphemeral("/" + this.domain);

    }

    private void createEphemeral(String path) {
        String ephemeralPath = this.write(path + "/sheets_", this.serverURI, CreateMode.EPHEMERAL_SEQUENTIAL);

        if (ephemeralPath != null) {
            System.out.println("Created znode: " + ephemeralPath);
        }
    }

    private void createWatcher(String path) {
        this.getChildren(path, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                try {
                    List<String> lst = getChildren(path, this);
                    // Quicksort is fast, specially for small collections
                    Collections.sort(lst);

                    String primaryURI = new String(zk.getData(path + "/" + lst.get(0), false, null));
                    if (!getPrimary().equals(primaryURI)) {
                        System.out.println("Primary is now: " + primaryURI);
                        setPrimary(primaryURI);
                        resource.newPrimary(primaryURI);
                    }
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createPersistent(String path) {
        try {
            if (this.zk.exists(path, false) != null) {
                System.out.println(path + " already existed.");
                return;
            }

            String persistentPath = this.write(path, CreateMode.PERSISTENT);
            if (persistentPath != null) {
                System.out.println("Created znode: " + persistentPath);
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void setPrimary(String primaryURL) {
        this.primaryURL = primaryURL;
    }

    public String getPrimary() {
        return this.primaryURL;
    }

    private String write(String path, CreateMode mode) {
        try {
            return this.zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String write(String path, String value, CreateMode mode) {
        try {
            return this.zk.create(path, value.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> getChildren(String path, Watcher watch) {
        try {
            return this.zk.getChildren(path, watch);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println(event);
    }

}
