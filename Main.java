package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid ass version-control system.
 *  @author Song Bai
 */

public class Main {

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** .gitlet folder. */
    static final File GITLET = new File(".gitlet");

    /** Staging for addition folder. */
    static final File STAGINGADDITION = Utils.join(GITLET, "stagingaddition");

    /** Staging for removal folder. */
    static final File STAGINGREMOVAL = Utils.join(GITLET, "stagingremoval");

    /** Commits folder. */
    static final File COMMITS = Utils.join(GITLET, "commits");

    /** Blobs folder. */
    static final File BLOBS = Utils.join(GITLET, "blobs");

    /** Branches folder. */
    static final File BRANCHES = Utils.join(GITLET, "branches");

    /** Current branch file. */
    static final File CURRENTBRANCH = Utils.join(GITLET, "currentbranch");

    /** Head commit file. */
    static final File HEAD = Utils.join(GITLET, "head");

    /** The repository object when main is called. */
    static final Repository REPO = new Repository();

    /** Input arguments. */
    private static String[] _args;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        _args = args;

        if (_args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        } else if (_args[0].equals("init")) {
            init();
            return;
        } else if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        switch (_args[0]) {
        case "add":
            add();
            break;
        case "commit":
            commit();
            break;
        case "rm":
            rm();
            break;
        case "log":
            log();
            break;
        case "global-log":
            globalLog();
            break;
        case "find":
            find();
            break;
        case "status":
            status();
            break;
        case "checkout":
            checkout();
            break;
        case "branch":
            branch();
            break;
        case "rm-branch":
            rmBranch();
            break;
        case "reset":
            reset();
            break;
        case "merge":
            merge();
            break;
        default:
            System.out.println("No command with that name exists.");
        }

    }

    /** Returns the commit with COMMITSHA. */
    private static Commit getCommit(String commitSHA) {
        File commitFile = Utils.join(COMMITS, commitSHA);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        return commit;
    }

    /** Returns head commit. */
    private static Commit getHeadCommit() {
        String headCommitSHA = Utils.readContentsAsString(HEAD);
        return getCommit(headCommitSHA);
    }

    /** Returns the name of the current branch. */
    private static String getCurrentBranch() {
        String currentBranch = Utils.readContentsAsString(CURRENTBRANCH);
        return currentBranch;
    }

    /** Returns the head commit of BRANCH. */
    private static Commit getBranchHeadCommit(String branch) {
        File branchFile = Utils.join(BRANCHES, branch);
        String branchHeadCommitSHA = Utils.readContentsAsString(branchFile);
        Commit branchHeadCommit = getCommit(branchHeadCommitSHA);
        return branchHeadCommit;
    }

    /** Returns SHA of the current working file with name FILENAME. */
    private static String getCWDFileSHA(String fileName) {
        File cwdFile = new File(fileName);
        byte[] cwdFileContents = Utils.readContents(cwdFile);
        String cwdFileSHA = Utils.sha1(cwdFileContents);
        return cwdFileSHA;
    }

    /** Returns true if checking out to COMMIT would overwrite files
     *  in the current working directory, else false. */
    private static boolean wouldOverwriteCWDFiles(Commit commit) {
        List<String> cwdFileList = Utils.plainFilenamesIn(CWD);

        for (String cwdFileName : cwdFileList) {
            String cwdFileSHA = getCWDFileSHA(cwdFileName);
            if (!getHeadCommit().blobs().containsKey(cwdFileName)
                    && commit.blobs().containsKey(cwdFileName)
                    && !commit.blobs().get(cwdFileName).equals(cwdFileSHA)) {
                return true;
            }
        }

        return false;
    }

    /** Init helper. */
    private static void init() throws IOException {
        if (_args.length != 1) {
            System.out.println("Incorrect operands.");
        } else if (GITLET.exists()) {
            System.out.println("A Gitlet version-control system"
                    + " already exists in the current directory.");
        } else {
            REPO.init();
        }
    }

    /** Add helper. */
    private static void add() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String fileName = _args[1];
            File fileToAdd = new File(fileName);

            if (!fileToAdd.exists()) {
                System.out.println("File does not exist.");
            } else {
                REPO.add(fileName, fileToAdd);
            }
        }
    }

    /** Commit helper. */
    private static void commit() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else if (_args[1].length() == 0) {
            System.out.println("Please enter a commit message.");
        } else if (STAGINGADDITION.listFiles().length == 0
                && STAGINGREMOVAL.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
        } else {
            String commitMessage = _args[1];
            REPO.commit(commitMessage);
        }
    }

    /** Rm helper. */
    private static void rm() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String fileName = _args[1];
            File fileToRemoveInStagingAddition
                    = Utils.join(STAGINGADDITION, fileName);

            if (!fileToRemoveInStagingAddition.exists()
                    && !getHeadCommit().blobs().containsKey(fileName)) {
                System.out.println("No reason to remove the file.");
            } else {
                REPO.rm(fileName);
            }
        }
    }

    /** Log helper. */
    private static void log() {
        if (_args.length != 1) {
            System.out.println("Incorrect operands.");
        } else {
            REPO.log();
        }
    }

    /** Globallog helper. */
    private static void globalLog() {
        if (_args.length != 1) {
            System.out.println("Incorrect operands.");
        } else {
            REPO.globalLog();
        }
    }

    /** Find helper. */
    private static void find() {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String commitMessage = _args[1];
            REPO.find(commitMessage);
        }
    }

    /** Status helper. */
    private static void status() {
        if (_args.length != 1) {
            System.out.println("Incorrect operands.");
        } else {
            REPO.status();
        }
    }

    /** Checkout helper. */
    private static void checkout() throws IOException {
        if (_args.length == 3) {
            checkout1();
        } else if (_args.length == 4) {
            checkout2();
        } else if (_args.length == 2) {
            checkout3();
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Checkout1 helper. */
    private static void checkout1() throws IOException {
        if (!_args[1].equals("--")) {
            System.out.println("Incorrect operands.");
        } else {
            String fileName = _args[2];
            if (!getHeadCommit().blobs().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
            } else {
                REPO.checkout1(fileName);
            }
        }
    }

    /** Checkout2 helper. */
    private static void checkout2() throws IOException {
        if (!_args[2].equals("--")) {
            System.out.println("Incorrect operands.");
        } else {
            String commitSHAPossiblyAbbreviated = _args[1];
            String fileName = _args[3];
            List<String> commitList = Utils.plainFilenamesIn(COMMITS);

            String fullCommitSHA = null;
            for (String commitSHA : commitList) {
                if (commitSHA.equals(commitSHAPossiblyAbbreviated)
                        || commitSHA.contains(commitSHAPossiblyAbbreviated)) {
                    fullCommitSHA = commitSHA;
                    break;
                }
            }

            if (fullCommitSHA == null) {
                System.out.println("No commit with that id exists.");
            } else {
                Commit commit = getCommit(fullCommitSHA);
                if (!commit.blobs().containsKey(fileName)) {
                    System.out.println("File does not exist in that commit.");
                } else {
                    REPO.checkout2(commit, fileName);
                }
            }
        }
    }

    /** Checkout3 helper. */
    private static void checkout3() throws IOException {
        String branch = _args[1];
        List<String> branchList = Utils.plainFilenamesIn(BRANCHES);
        if (!branchList.contains(branch)) {
            System.out.println("No such branch exists.");
        } else if (branch.equals(getCurrentBranch())) {
            System.out.println("No need to checkout the current branch.");
        } else if (wouldOverwriteCWDFiles(getBranchHeadCommit(branch))) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
        } else {
            REPO.checkout3(branch);
        }
    }

    /** Branch helper. */
    public static void branch() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String branch = _args[1];
            List<String> branchList = Utils.plainFilenamesIn(BRANCHES);
            if (branchList.contains(branch)) {
                System.out.println("A branch with that name already exists.");
            } else {
                REPO.branch(branch);
            }
        }
    }

    /** Rmbranch helper. */
    private static void rmBranch() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String branch = _args[1];
            List<String> branchList = Utils.plainFilenamesIn(BRANCHES);
            if (!branchList.contains(branch)) {
                System.out.println("A branch with that name does not exist.");
            } else if (branch.equals(getCurrentBranch())) {
                System.out.println("Cannot remove the current branch.");
            } else {
                REPO.rmBranch(branch);
            }
        }
    }

    /** Reset helper. */
    private static void reset() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String commitSHAPossiblyAbbreviated = _args[1];
            List<String> commitList = Utils.plainFilenamesIn(COMMITS);

            String fullCommitSHA = null;
            for (String commitSHA : commitList) {
                if (commitSHA.equals(commitSHAPossiblyAbbreviated)
                        || commitSHA.contains(commitSHAPossiblyAbbreviated)) {
                    fullCommitSHA = commitSHA;
                    break;
                }
            }

            if (fullCommitSHA == null) {
                System.out.println("No commit with that id exists.");
            } else if (wouldOverwriteCWDFiles(getCommit(fullCommitSHA))) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            } else {
                REPO.reset(fullCommitSHA);
            }
        }
    }

    /** Reset helper. */
    private static void merge() throws IOException {
        if (_args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String branch = _args[1];
            List<String> branchList = Utils.plainFilenamesIn(BRANCHES);
            List<String> stagingAdditionList
                    = Utils.plainFilenamesIn(STAGINGADDITION);
            List<String> stagingRemovalList
                    = Utils.plainFilenamesIn(STAGINGREMOVAL);
            if (stagingAdditionList.size() != 0
                    || stagingRemovalList.size() != 0) {
                System.out.println("You have uncommitted changes.");
            } else if (!branchList.contains(branch)) {
                System.out.println("A branch with that name does not exist.");
            } else if (branch.equals(getCurrentBranch())) {
                System.out.println("Cannot merge a branch with itself.");
            } else {
                REPO.merge(branch);
            }
        }
    }
}
