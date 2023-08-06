package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Repository class for Gitlet, the tiny stupid ass version-control system.
 *  @author Song Bai
 *
 *  credits:
 *  https://stackoverflow.com/questions/5930087/
 *  how-to-check-if-a-directory-is-empty-in-java
 *  https://docs.oracle.com/javase/7/docs/api/
 *  java/lang/String.html#substring(int,%20int)
 *  https://stackoverflow.com/questions/16252269/how-to-sort-an-arraylist
 */

public class Repository implements Serializable {

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

    /** Creates a repository object. */
    public Repository() {
    }

    /** Saves COMMIT to the repo. */
    private static void saveCommit(Commit commit) throws IOException {
        byte[] commitSerialized = Utils.serialize(commit);
        String commitSHA = getCommitSHA(commit);

        File commitFile = Utils.join(COMMITS, commitSHA);
        commitFile.createNewFile();
        Utils.writeContents(commitFile, commitSerialized);
    }

    /** Returns the commit with COMMITSHA. */
    private static Commit getCommit(String commitSHA) {
        File commitFile = Utils.join(COMMITS, commitSHA);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        return commit;
    }

    /** Returns the parent commit of COMMIT. */
    private static Commit getParentCommit(Commit commit) {
        String parentCommitSHA = commit.parent();
        if (parentCommitSHA == null) {
            return null;
        } else {
            File parentCommitFile = Utils.join(COMMITS, parentCommitSHA);
            Commit parentCommit = Utils.readObject(parentCommitFile,
                    Commit.class);
            return parentCommit;
        }
    }

    /** Returns the head commit. */
    private static Commit getHeadCommit() {
        String headCommitSHA = Utils.readContentsAsString(HEAD);
        return getCommit(headCommitSHA);
    }

    /** Returns the commitSHA of COMMIT. */
    private static String getCommitSHA(Commit commit) {
        byte[] commitSerialized = Utils.serialize(commit);
        String commitSHA = Utils.sha1(commitSerialized);
        return commitSHA;
    }

    /** Moves the current branch pointer to the COMMIT. */
    private static void updateCurrentBranch(Commit commit) throws IOException {
        String commitSHA = getCommitSHA(commit);

        String currentBranch = Utils.readContentsAsString(CURRENTBRANCH);
        File currentBranchFile = Utils.join(BRANCHES, currentBranch);
        Utils.writeContents(currentBranchFile, commitSHA);
    }

    /** Moves the current branch pointer to the commit with COMMITSHA. */
    private static void updateCurrentBranch(String commitSHA)
            throws IOException {
        Commit commit = getCommit(commitSHA);
        updateCurrentBranch(commit);
    }

    /** Returns the current branch's name. */
    private static String getCurrentBranch() {
        String currentBranch = Utils.readContentsAsString(CURRENTBRANCH);
        return currentBranch;
    }

    /** Moves head commit pointer to the SHA of COMMIT. */
    private static void updateHead(Commit commit) {
        String commitSHA = getCommitSHA(commit);
        Utils.writeContents(HEAD, commitSHA);
    }

    /** Displays relevant info of COMMIT. */
    private static void displayCommitInfo(Commit commit) {
        System.out.println("===");
        String commitSHA = getCommitSHA(commit);
        System.out.println("commit " + commitSHA);
        if (commit.parent2() != null) {
            String parent1Abbreviated = commit.parent().substring(0, 7);
            String parent2Abbreviated = commit.parent2().substring(0, 7);
            System.out.println("Merge: "
                    + parent1Abbreviated + " " + parent2Abbreviated);
        }
        System.out.println("Date: " + commit.timestamp());
        System.out.println(commit.message());
    }

    /** Clears the staging area. */
    private static void clearStagingArea() {
        for (File file : STAGINGADDITION.listFiles()) {
            file.delete();
        }
        for (File file : STAGINGREMOVAL.listFiles()) {
            file.delete();
        }
    }

    /** Returns the blob contents of the file with FILENAME in COMMIT. */
    private static byte[] getBlobContents(String fileName, Commit commit) {
        String fileInCommitSHA = commit.blobs().get(fileName);
        File fileInCommitBlobFile = Utils.join(BLOBS, fileInCommitSHA);
        byte[] fileInHeadCommitContents
                = Utils.readContents(fileInCommitBlobFile);
        return fileInHeadCommitContents;
    }

    /** Writes CONTENTS to the file in the working directory with FILENAME. */
    private static void writeToFileInCWD(String fileName, byte[] contents)
            throws IOException {
        File fileInCWD = new File(fileName);
        fileInCWD.createNewFile();
        Utils.writeContents(fileInCWD, contents);
    }

    /** Returns the head commit of BRANCH. */
    private static Commit getBranchHeadCommit(String branch) {
        File branchFile = Utils.join(BRANCHES, branch);
        String branchHeadCommitSHA = Utils.readContentsAsString(branchFile);
        Commit branchHeadCommit = getCommit(branchHeadCommitSHA);
        return branchHeadCommit;
    }

    /** Returns SHA of the working file with name FILENAME. */
    private static String getCWDFileSHA(String fileName) {
        File cwdFile = new File(fileName);
        byte[] cwdFileContents = Utils.readContents(cwdFile);
        String cwdFileSHA = Utils.sha1(cwdFileContents);
        return cwdFileSHA;
    }

    /** Returns true if checking out to COMMIT would overwrite files in the
     * current working directory, else false. */
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

    /** Initializes a gitlet repository in the current working directory. */
    public static void init() throws IOException {
        GITLET.mkdir();
        STAGINGADDITION.mkdir();
        STAGINGREMOVAL.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
        BRANCHES.mkdir();
        CURRENTBRANCH.createNewFile();
        HEAD.createNewFile();

        Commit initialCommit = new Commit();
        byte[] initialCommitSerialized = Utils.serialize(initialCommit);
        String initialCommitSHA = getCommitSHA(initialCommit);

        File initialCommitFile = Utils.join(COMMITS, initialCommitSHA);
        initialCommitFile.createNewFile();
        Utils.writeContents(initialCommitFile, initialCommitSerialized);

        File masterBranchFile = Utils.join(BRANCHES, "master");
        masterBranchFile.createNewFile();
        Utils.writeContents(masterBranchFile, initialCommitSHA);

        Utils.writeContents(CURRENTBRANCH, "master");

        Utils.writeContents(HEAD, initialCommitSHA);
    }

    /** Stages the file with FILENAME and filepath FILETOADD for addition. */
    public static void add(String fileName, File fileToAdd)
            throws IOException {
        byte[] fileContents = Utils.readContents(fileToAdd);
        String blobSHA = Utils.sha1(fileContents);

        File blobFile = Utils.join(BLOBS, blobSHA);
        blobFile.createNewFile();
        Utils.writeContents(blobFile, fileContents);

        File stagingAreaAdditionFile = Utils.join(STAGINGADDITION, fileName);
        File stagingAreaRemovalFile = Utils.join(STAGINGREMOVAL, fileName);

        if (stagingAreaAdditionFile.exists()) {
            stagingAreaAdditionFile.delete();
        }
        if (stagingAreaRemovalFile.exists()) {
            stagingAreaRemovalFile.delete();
        }

        String headCommitFileBlobSHA = getHeadCommit().blobs().get(fileName);

        if (!blobSHA.equals(headCommitFileBlobSHA)) {
            stagingAreaAdditionFile.createNewFile();
            Utils.writeContents(stagingAreaAdditionFile, blobSHA);
        }
    }

    /** Makes a commit with message COMMITMESSAGE. */
    public static void commit(String commitMessage) throws IOException {
        Commit commit = new Commit(commitMessage);
        saveCommit(commit);
        updateCurrentBranch(commit);
        updateHead(commit);
        clearStagingArea();
    }

    /** Makes a merge commit with 2nd parent PARENT2 and
     * message COMMITMESSAGE. */
    public static void mergeCommit(String parent2, String commitMessage)
            throws IOException {
        Commit commit = new Commit(parent2, commitMessage);
        saveCommit(commit);
        updateCurrentBranch(commit);
        updateHead(commit);
        clearStagingArea();
    }

    /** Removes the file with FILENAME. */
    public static void rm(String fileName) throws IOException {
        File fileToRemoveInStagingAddition
                = Utils.join(STAGINGADDITION, fileName);
        if (fileToRemoveInStagingAddition.exists()) {
            fileToRemoveInStagingAddition.delete();
        }

        if (getHeadCommit().blobs().containsKey(fileName)) {
            File fileToRemoveInStagingRemoval
                    = Utils.join(STAGINGREMOVAL, fileName);
            fileToRemoveInStagingRemoval.createNewFile();

            Utils.restrictedDelete(fileName);
        }
    }

    /** Prints the log. */
    public static void log() {
        Commit currentCommit = getHeadCommit();
        while (true) {
            displayCommitInfo(currentCommit);

            System.out.println();

            if (currentCommit.parent() == null) {
                return;
            }

            currentCommit = getParentCommit(currentCommit);
        }
    }

    /** Prints the global log. */
    public static void globalLog() {
        List<String> commitList = Utils.plainFilenamesIn(COMMITS);
        for (int i = 0; i < commitList.size(); i += 1) {
            String commitSHA = commitList.get(i);
            Commit commit = getCommit(commitSHA);
            displayCommitInfo(commit);
            System.out.println();
        }
    }

    /** Prints the commit SHA(s) with COMMITMESSAGE. */
    public static void find(String commitMessage) {
        List<String> commitList = Utils.plainFilenamesIn(COMMITS);
        boolean foundCommit = false;

        for (String commitSHA : commitList) {
            Commit commit = getCommit(commitSHA);
            if (commit.message().equals(commitMessage)) {
                System.out.println(commitSHA);
                foundCommit = true;
            }
        }

        if (!foundCommit) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Prints the status of the repo. */
    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branchList = Utils.plainFilenamesIn(BRANCHES);
        String currentBranch = getCurrentBranch();
        for (String branch : branchList) {
            if (branch.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        List<String> stagingAdditionList
                = Utils.plainFilenamesIn(STAGINGADDITION);
        for (String fileName : stagingAdditionList) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        List<String> stagingRemovalList
                = Utils.plainFilenamesIn(STAGINGREMOVAL);
        for (String fileName : stagingRemovalList) {
            System.out.println(fileName);
        }
        System.out.println();

        printModificationsNotStagedForCommit();
        System.out.println();

        printUntrackedFiles();
        System.out.println();
    }

    /** Helper for status. */
    public static void printModificationsNotStagedForCommit() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> toPrint = new ArrayList<>();

        for (String fileName : getHeadCommit().blobs().keySet()) {
            String trackedFileSHA = getHeadCommit().blobs().get(fileName);
            File fileInCWD = new File(fileName);
            File fileInStagingAddition = Utils.join(STAGINGADDITION, fileName);
            File fileInStagingRemoval = Utils.join(STAGINGREMOVAL, fileName);

            if (fileInCWD.exists()
                    && !fileInStagingAddition.exists()
                    && !fileInStagingRemoval.exists()) {
                String fileInCWDSHA = getCWDFileSHA(fileName);
                if (!fileInCWDSHA.equals(trackedFileSHA)) {
                    toPrint.add(fileName + " (modified)");
                }
            }
        }

        for (File file : STAGINGADDITION.listFiles()) {
            String fileName = file.getName();
            String fileInStagingAdditionSHA = Utils.readContentsAsString(file);
            File fileInCWD = new File(fileName);
            if (fileInCWD.exists()) {
                String fileInCWDSHA = getCWDFileSHA(fileName);
                if (!fileInCWDSHA.equals(fileInStagingAdditionSHA)) {
                    toPrint.add(fileName + " (modified)");
                }
            }
        }

        for (File file : STAGINGADDITION.listFiles()) {
            String fileName = file.getName();
            File fileInCWD = new File(fileName);
            if (!fileInCWD.exists()) {
                toPrint.add(fileName + " (deleted)");
            }
        }

        for (String fileName : getHeadCommit().blobs().keySet()) {
            File fileInCWD = new File(fileName);
            File fileInStagingRemoval = Utils.join(STAGINGREMOVAL, fileName);
            if (!fileInCWD.exists() && !fileInStagingRemoval.exists()) {
                toPrint.add(fileName + " (deleted)");
            }
        }

        Collections.sort(toPrint);
        for (String fileName : toPrint) {
            System.out.println(fileName);
        }
    }

    /** Helper for status. */
    public static void printUntrackedFiles() {
        System.out.println("=== Untracked Files ===");

        List<String> cwdList = Utils.plainFilenamesIn(CWD);
        for (String fileName : cwdList) {
            File fileInStagingAddition = Utils.join(STAGINGADDITION, fileName);
            if (!fileInStagingAddition.exists()
                    && !getHeadCommit().blobs().containsKey(fileName)) {
                System.out.println(fileName);
            }
        }

    }

    /** Checkout #1, for file with FILENAME. */
    public static void checkout1(String fileName) throws IOException {
        byte[] fileInHeadCommitContents
                = getBlobContents(fileName, getHeadCommit());
        writeToFileInCWD(fileName, fileInHeadCommitContents);
    }

    /** Checkout #2, for COMMIT and file with FILENAME. */
    public static void checkout2(Commit commit, String fileName)
            throws IOException {
        byte[] fileInCommitContents = getBlobContents(fileName, commit);
        writeToFileInCWD(fileName, fileInCommitContents);
    }

    /** Checkout #3, for branch with name BRANCH. */
    public static void checkout3(String branch) throws IOException {
        Commit branchHeadCommit = getBranchHeadCommit(branch);
        for (String fileName : branchHeadCommit.blobs().keySet()) {
            byte[] fileInBranchHeadCommitContents
                    = getBlobContents(fileName, branchHeadCommit);
            writeToFileInCWD(fileName, fileInBranchHeadCommitContents);
        }

        for (String fileName : getHeadCommit().blobs().keySet()) {
            if (!branchHeadCommit.blobs().containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
            }
        }

        clearStagingArea();

        Utils.writeContents(CURRENTBRANCH, branch);
        Utils.writeContents(HEAD, getCommitSHA(branchHeadCommit));
    }

    /** Creates new branch with name BRANCH. */
    public static void branch(String branch) throws IOException {
        File branchFile = Utils.join(BRANCHES, branch);
        branchFile.createNewFile();
        Utils.writeContents(branchFile, getCommitSHA(getHeadCommit()));
    }

    /** Removes branch with name BRANCH. */
    public static void rmBranch(String branch) throws IOException {
        File branchFile = Utils.join(BRANCHES, branch);
        branchFile.delete();
    }

    /** Resets repo to commit with SHA COMMITSHA. */
    public static void reset(String commitSHA) throws IOException {
        updateCurrentBranch(commitSHA);
        checkout3(getCurrentBranch());
    }

    /** Merges current branch with BRANCH. */
    public static void merge(String branch) throws IOException {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        if (wouldOverwriteCWDFiles(givenBranchHeadCommit)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        } else if (splitPointCommit.equals(givenBranchHeadCommit)) {
            System.out.println("Given branch is an ancestor of "
                    + "the current branch.");
            return;
        } else if (splitPointCommit.equals(currentBranchHeadCommit)) {
            checkout3(branch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        mergeHelper1(branch);
        mergeHelper2(branch);
        mergeHelper3(branch);

        boolean isConflict1 = mergeHelper4(branch);
        boolean isConflict2 = mergeHelper5(branch);
        boolean isConflict3 = mergeHelper6(branch);
        boolean isConflict4 = mergeHelper7(branch);

        if (!splitPointCommit.equals(currentBranchHeadCommit)
                && !splitPointCommit.equals(givenBranchHeadCommit)) {
            mergeCommit(getCommitSHA(givenBranchHeadCommit),
                    "Merged "
                            + branch + " into " + getCurrentBranch() + ".");
        }

        if (isConflict1 || isConflict2 || isConflict3 || isConflict4) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Merge helper 1 with branch BRANCH. */
    private static void mergeHelper1(String branch) throws IOException {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        for (String fileName : splitPointCommit.blobs().keySet()) {
            if (currentBranchHeadCommit.blobs().containsKey(fileName)
                    && givenBranchHeadCommit.blobs().containsKey(fileName)
                    && !givenBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))
                    && currentBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))) {
                checkout2(givenBranchHeadCommit, fileName);
                File fileInStagingAdditionFile
                        = Utils.join(STAGINGADDITION, fileName);
                fileInStagingAdditionFile.createNewFile();
                Utils.writeContents(fileInStagingAdditionFile,
                        givenBranchHeadCommit.blobs().get(fileName));
            }
        }
    }

    /** Merge helper 2 with branch BRANCH. */
    private static void mergeHelper2(String branch) throws IOException {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        for (String fileName : givenBranchHeadCommit.blobs().keySet()) {
            if (!splitPointCommit.blobs().containsKey(fileName)
                    && !currentBranchHeadCommit.blobs().
                    containsKey(fileName)) {
                checkout2(givenBranchHeadCommit, fileName);
                File fileInStagingAdditionFile
                        = Utils.join(STAGINGADDITION, fileName);
                fileInStagingAdditionFile.createNewFile();
                Utils.writeContents(fileInStagingAdditionFile,
                        givenBranchHeadCommit.blobs().get(fileName));
            }
        }
    }

    /** Merge helper 3 with branch BRANCH. */
    private static void mergeHelper3(String branch) throws IOException {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        for (String fileName : splitPointCommit.blobs().keySet()) {
            if (currentBranchHeadCommit.blobs().containsKey(fileName)
                    && currentBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))
                    && givenBranchHeadCommit.blobs().containsKey(fileName)) {
                rm(fileName);
            }
        }
    }

    /** Merge helper 4 with branch BRANCH. Returns true is there
     * is a conflict, false otherwise. */
    private static boolean mergeHelper4(String branch) {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        boolean isConflict = false;

        for (String fileName : splitPointCommit.blobs().keySet()) {
            if (currentBranchHeadCommit.blobs().containsKey(fileName)
                    && !currentBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))
                    && givenBranchHeadCommit.blobs().containsKey(fileName)
                    && !givenBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))
                    && !currentBranchHeadCommit.blobs().get(fileName).
                    equals(givenBranchHeadCommit.blobs().get(fileName))) {
                isConflict = true;

                File currentBranchFileContentsFile
                        = Utils.join(BLOBS,
                        currentBranchHeadCommit.blobs().get(fileName));
                byte[] currentBranchFileContents
                        = Utils.readContents(currentBranchFileContentsFile);

                File givenBranchFileContentsFile
                        = Utils.join(BLOBS,
                        givenBranchHeadCommit.blobs().get(fileName));
                byte[] givenBranchFileContents
                        = Utils.readContents(givenBranchFileContentsFile);

                File fileInCWD = new File(fileName);
                Utils.writeContents(fileInCWD, "<<<<<<< HEAD\n",
                        currentBranchFileContents,
                        "=======\n",
                        givenBranchFileContents,
                        ">>>>>>>\n");
            }
        }
        return isConflict;
    }

    /** Merge helper 5 with branch BRANCH. Returns true if
     * there is a conflict, false otherwise. */
    private static boolean mergeHelper5(String branch) {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        boolean isConflict = false;

        for (String fileName : splitPointCommit.blobs().keySet()) {
            if (currentBranchHeadCommit.blobs().containsKey(fileName)
                    && !currentBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))
                    && !givenBranchHeadCommit.blobs().containsKey(fileName)) {

                isConflict = true;

                File currentBranchFileContentsFile
                        = Utils.join(BLOBS,
                        currentBranchHeadCommit.blobs().get(fileName));
                byte[] currentBranchFileContents
                        = Utils.readContents(currentBranchFileContentsFile);

                File fileInCWD = new File(fileName);
                Utils.writeContents(fileInCWD, "<<<<<<< HEAD\n",
                        currentBranchFileContents,
                        "=======\n",
                        ">>>>>>>\n");
            }
        }
        return isConflict;
    }

    /** Merge helper 6 with branch BRANCH. Returns true
     * if there is a conflict, false otherwise. */
    private static boolean mergeHelper6(String branch) {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        boolean isConflict = false;

        for (String fileName : splitPointCommit.blobs().keySet()) {
            if (!currentBranchHeadCommit.blobs().containsKey(fileName)
                    && givenBranchHeadCommit.blobs().containsKey(fileName)
                    && !givenBranchHeadCommit.blobs().get(fileName).
                    equals(splitPointCommit.blobs().get(fileName))) {
                isConflict = true;

                File givenBranchFileContentsFile
                        = Utils.join(BLOBS,
                        givenBranchHeadCommit.blobs().get(fileName));
                byte[] givenBranchFileContents
                        = Utils.readContents(givenBranchFileContentsFile);

                File fileInCWD = new File(fileName);
                Utils.writeContents(fileInCWD, "<<<<<<< HEAD\n",
                        "=======\n",
                        givenBranchFileContents,
                        ">>>>>>>\n");
            }
        }
        return isConflict;
    }

    /** Merge helper 7 with branch BRANCH. Returns true
     * is there is a conflict, false otherwise. */
    private static boolean mergeHelper7(String branch) {
        Commit splitPointCommit = findSplitPoint(branch);
        Commit givenBranchHeadCommit = getBranchHeadCommit(branch);
        Commit currentBranchHeadCommit = getHeadCommit();

        boolean isConflict = false;

        for (String fileName : currentBranchHeadCommit.blobs().keySet()) {
            if (givenBranchHeadCommit.blobs().containsKey(fileName)
                    && !splitPointCommit.blobs().containsKey(fileName)
                    && !currentBranchHeadCommit.blobs().get(fileName).
                    equals(givenBranchHeadCommit.blobs().get(fileName))) {
                isConflict = true;

                File currentBranchFileContentsFile
                        = Utils.join(BLOBS,
                        currentBranchHeadCommit.blobs().get(fileName));
                byte[] currentBranchFileContents
                        = Utils.readContents(currentBranchFileContentsFile);

                File givenBranchFileContentsFile
                        = Utils.join(BLOBS,
                        givenBranchHeadCommit.blobs().get(fileName));
                byte[] givenBranchFileContents
                        = Utils.readContents(givenBranchFileContentsFile);

                File fileInCWD = new File(fileName);
                Utils.writeContents(fileInCWD, "<<<<<<< HEAD\n",
                        currentBranchFileContents,
                        "=======\n",
                        givenBranchFileContents,
                        ">>>>>>>\n");
            }
        }
        return isConflict;
    }

    /** Returns the split point commit of the current branch
     * and given BRANCH. */
    private static Commit findSplitPoint(String branch) {
        ArrayList<String> givenBranchParentList = new ArrayList<>();
        Commit givenBranchCommit = getBranchHeadCommit(branch);
        ArrayDeque<Commit> givenQueue = new ArrayDeque<>();
        givenQueue.push(givenBranchCommit);
        while (!givenQueue.isEmpty()) {
            Commit commit = givenQueue.remove();
            givenBranchParentList.add(getCommitSHA(commit));
            if (commit.parent() != null) {
                givenQueue.push(getCommit(commit.parent()));
            }
            if (commit.parent2() != null) {
                givenQueue.push(getCommit(commit.parent2()));
            }
        }

        Commit currentBranchCommit = getHeadCommit();
        ArrayDeque<Commit> currentQueue = new ArrayDeque<>();
        currentQueue.push(currentBranchCommit);
        while (!currentQueue.isEmpty()) {
            Commit commit = currentQueue.remove();
            if (givenBranchParentList.contains(getCommitSHA(commit))) {
                return commit;
            }
            if (commit.parent() != null) {
                currentQueue.push(getCommit(commit.parent()));
            }
            if (commit.parent2() != null) {
                currentQueue.push(getCommit(commit.parent2()));
            }
        }
        return null;
    }
}
