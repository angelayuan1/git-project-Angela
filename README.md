# git-project-Angela
Call the initRepo() method in the main to initialize a git directory and an objects directory, HEAD file, and index file inside it.
You can initialize, verify, and cleanup the repository through calling methods to test if it can be created and deleted. 
A SHA-1 hash function was created to create a hexadecimal string representing the filpath
To create BLOBs call the blob() method on any file you want to track, creating a file in the objects directory named after the file’s SHA-1 hash and containing the content of your original file.
To add files to the index use addToIdx(hashFile("path/to/your/file"), "filename.txt"). 
To reset or clean up the repository for testing, reset() clears everything in the objects foler and refreshes index, while cleanUp() removes the entire git folder.
the tester will initialize the repo if it doesn’t exist, create sample text files, blob them & add entries to index file, and reset.
# Cyrus Additions
genTreesFromIdx()
Fixed so if no subdirs remain, collapse all top-level items into a root tree

In GP-4.1, functionality was added to trace and verify the root tree that gets built from the index file.  
Helper methods `readObjectText()` and `traceTree()` in `GitTester.java` recursively print and verify tree contents.  
This ensures all blobs and subtrees appear in the correct structure under the root tree, confirming proper linking between files and directories.  

In GP-4.2, commit functionality was added to record snapshots of the repository.  
The `commit(String author, String message)` method in `Git.java` generates a commit file that references the root tree and (if present) the previous commit from the `HEAD` file.  
Each commit file includes the tree SHA, parent SHA, author name, timestamp, and commit message, then updates `HEAD` to point to the newest commit.  
This allows a full chain of commits to be formed over time, preserving the history of changes.  

In GP-4.3, a new wrapper class `GitRekt` was created to make using the Git system simpler.  
It provides easy-to-use methods like `init()`, `add()`, `commit()`, `checkout()`, and `reset()` to standardize interaction with the repository.  
A new test block in `GitTester.java` verifies `GitRekt` by creating files, staging them, committing twice, and printing commit hashes to confirm that `HEAD` updates correctly after each commit.  

Together, these updates allow the repository to:  
- Build and trace tree hierarchies from the index.  
- Create and link commits with full metadata.  
- Provide a unified user interface for initialization, staging, and committing via `GitRekt`.  