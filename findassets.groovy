@Grab(group='org.apache.commons', module='commons-io', version='1.3.2')
import org.apache.commons.io.FilenameUtils;

import groovy.io.FileType

class UnusuedAssetFinder {

    def allFiles = [:]

    File homeDir;

    public UnusuedAssetFinder(String homedir) {

        homeDir = new File(homedir)

        homeDir.eachFileRecurse (FileType.FILES) { aFile ->

            if (aFile.toString().contains("/.")) { return }

            if (!aFile.isHidden() && !aFile.name.startsWith('.'))  {
                allFiles += [ (aFile.getCanonicalPath()) : aFile ]
            }

        }

        File file = new File('index.html');

        findAllAssetsForFile(file)

        File unusued = new File('unusedfiles.txt')

        unusued.write('')

        allFiles.each {
            println it.getValue().getPath()
            unusued.append(it.getValue().getPath() + "\n")
        }
    }

    void removeFileForCanonicalPath(String canonicalPath) {

        println "--removing file: " + canonicalPath

        allFiles.remove(canonicalPath)

    }

    def removeAssetForRelativePath(String relativePath) {

        File file = new File(relativePath);

        String canonicalPath = file.getCanonicalPath();

        removeFileForCanonicalPath(canonicalPath)

    }

    def findAllAssetsForFile(File file) {

        final HREF_REGEX = /(?i)href=['"]([^'"]*)['"]/
        final SRC_REGEX = /(?i)src=['"]([^'"]*)['"]/
        final CSS_URL_REGEX = /(?i)url\(([^\)]*)\)/

        def hrefs = []
        def srcs = []
        def cssUrls = []

        def all = []

        String fileText = file.text

        fileText.findAll(HREF_REGEX) {

            hrefs += it[1]

            removeAssetForRelativePath(it[1])

            if (it[1].find(/.css$/) && !it[1].find(/^HTTP/)) {

                File fileCss = new File(it[1])

                String fileFullPath = FilenameUtils.getFullPath(fileCss.getCanonicalPath())

                fileCss.text.findAll(CSS_URL_REGEX) {

                    println "Found CSS Url: " + it[1]
                    File fileCssUrl = new File(fileFullPath + it[1]);

                    removeFileForCanonicalPath(fileCssUrl.getCanonicalPath())

                }

            }
        }

        fileText.findAll(SRC_REGEX) {
            srcs += it[1]
            removeAssetForRelativePath(it[1])
        }

        all = hrefs + srcs

        all.toSet()

    }

}


def assetFinder = new UnusuedAssetFinder('.')