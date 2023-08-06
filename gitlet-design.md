# Gitlet Design Document

**Name**: Song Bai


## Classes and Data Structures

### .gitlet folder
* /commits folder
* /blobs folder
* /head file
* /branches folder
* /currentbranch file
* /stagingaddition folder
* /stagingremoval folder

### Main
* main() is for processing args
* individual command methods are for error checking and calling methods in Repository

### Repository (or Gitlet)
* implements Serializable
* is the repo
* has the methods to init, add, commit, log, checkout...
* contains commits, blobs, branch pointers, head pointer, staging area
* commits stored in .gitlet/commits folder
  * (filename: commit sha, contents: serialized commit)
* blobs stored in .gitlet/blobs folder
  * (file name:blob sha, contents: text) 
* branches stored in .gitlet/branches folder
  * a branch points to the most recent commit of that branch
  * (file name: branch name, contents: commit sha)
* current branch is in .gitlet/currentbranch file
  * contents: branch name
* HEAD pointer stored in .gitlet/head file
  * (contents: commit sha)
* files for staging stored in .gitlet/stagingaddition folder and .gitlet/stagingremoval folders
  * staging addition: (file name: file name, contents: blob sha)
  * staging removal: (file name: file name, contents: blank? don't need?)

### Commit 
* implements Serializable
* message (log message)
* id (serialize and sha) (used in repository)
* date n time
* author (do we need?)
* parent commit (parent's sha)
* parent2 commit (parent's sha) for merges n shit
* blobs (linked hashmap) (file name : blob sha)


## Algorithms
* some filler text for when I need algorithms.


## Persistence
* read and write things to their respective folders in .gitlet
* .gitlet folder
  * /commits folder use read object?
  * /blobs folder readcontents
  * /head file
  * /branches folder
  * /currentbranch file
  * /stagingaddition folder
  * /stagingremoval folder
* when call main(), have a repo in Main.class which will be used to do things


