## Contributing to Jupiter

Jupiter is released under the Apache 2.0 license, and follows a very
standard Github development process, using Github tracker for issues and
merging pull requests into master. If you would like to contribute something, 
or simply want to hack on the code this document should help you get started.

### Code Conventions
None of these is essential for a pull request, but they will all help.

1. Make sure all new `.java` files to have a simple Javadoc class comment
with at least an `@author` tag identifying you, and preferably at least a
paragraph on what the class is for.

2. Add the ASF license header comment to all new `.java` files (copy from 
existing files in the project)

3. Add yourself as an `@author` to the `.java` files that you modify 
substantially (more than cosmetic changes).

4. Add some Javadocs.

5. A few unit tests would help a lot as well — someone has to do it.

6. When writing a commit message please follow [these conventions](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html),
if you are fixing an existing issue please add Fixes gh-XXXX at the end of the commit message (where XXXX is the issue number).
