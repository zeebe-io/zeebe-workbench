// vim: set filetype=groovy:

def jobName = 'zeebe-workbench-DISTRO-maven-deploy'
def repository = 'zeebe-workbench'
def gitBranch = 'master'

def pom = 'pom.xml'
def mvnGoals = 'clean verify -B'

def mavenVersion = 'maven-3.3-latest'
def mavenSettingsId = 'camunda-maven-settings'

// script to set access rights on ssh keys
// and configure git user name and email
def setupGitConfig = '''\
#!/bin/bash -xe

chmod 600 ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa.pub

git config --global user.email "ci@camunda.com"
git config --global user.name "camunda-jenkins"
'''

def mavenGpgKeys = '''\
#!/bin/bash

if [ -e "${MVN_CENTRAL_GPG_KEY_SEC}" ]
then
  gpg -q --allow-secret-key-import --import ${MVN_CENTRAL_GPG_KEY_SEC} || echo 'Private GPG Sign Key is already imported!.'
  rm ${MVN_CENTRAL_GPG_KEY_SEC}
else
  echo 'Private GPG Key not found.'
fi

if [ -e "${MVN_CENTRAL_GPG_KEY_PUB}" ]
then
  gpg -q --import ${MVN_CENTRAL_GPG_KEY_PUB} || echo 'Public GPG Sign Key is already imported!.'
  rm ${MVN_CENTRAL_GPG_KEY_PUB}
else
  echo 'Public GPG Key not found.'
fi
'''

def githubRelease = '''\
#!/bin/bash

cd web-app/target

JAR="zeebe-workbench-${RELEASE_VERSION}.jar"
CHECKSUM="${JAR}.sha1sum"

# create checksum files
sha1sum ${JAR} > ${CHECKSUM}

# do github release
curl -sL https://github.com/aktau/github-release/releases/download/v0.7.2/linux-amd64-github-release.tar.bz2 | tar xjvf - --strip 3

./github-release release --user zeebe-io --repo zeebe-workbench --tag ${RELEASE_VERSION} --name "Zeebe Workbench ${RELEASE_VERSION}" --description ""
./github-release upload --user zeebe-io --repo zeebe-workbench --tag ${RELEASE_VERSION} --name "${JAR}" --file "${JAR}"
./github-release upload --user zeebe-io --repo zeebe-workbench --tag ${RELEASE_VERSION} --name "${CHECKSUM}" --file "${CHECKSUM}"
'''

// properties used by the release build
def releaseProperties = [
    resume: 'false',
    tag: '${RELEASE_VERSION}',
    releaseVersion: '${RELEASE_VERSION}',
    developmentVersion: '${DEVELOPMENT_VERSION}',
    pushChanges: '${PUSH_CHANGES}',
    remoteTagging: '${PUSH_CHANGES}',
    localCheckout: '${USE_LOCAL_CHECKOUT}',
    arguments: '--settings=${NEXUS_SETTINGS} -DskipTests=true -Dgpg.passphrase="${GPG_PASSPHRASE}" -Dskip.central.release=${SKIP_DEPLOY_TO_MAVEN_CENTRAL} -Dskip.camunda.release=${SKIP_DEPLOY_TO_CAMUNDA_NEXUS}',
]


mavenJob(jobName)
{
    scm
    {
        git
        {
            remote
            {
                github 'zeebe-io/' + repository, 'ssh'
                credentials 'camunda-jenkins-github-ssh'
            }
            branch gitBranch
            extensions
            {
                localBranch gitBranch
            }
        }
    }
    triggers
    {
        githubPush()
    }
    label 'ubuntu'
    jdk 'jdk-8-latest'

    rootPOM pom
    goals mvnGoals
    localRepository LocalRepositoryLocation.LOCAL_TO_WORKSPACE
    providedSettings mavenSettingsId
    mavenInstallation mavenVersion

    wrappers
    {
        timestamps()

        timeout
        {
            absolute 60
        }

        configFiles
        {
            // jenkins github public ssh key needed to push to github
            custom('Jenkins CI GitHub SSH Public Key')
            {
                targetLocation '/home/camunda/.ssh/id_rsa.pub'
            }
            // jenkins github private ssh key needed to push to github
            custom('Jenkins CI GitHub SSH Private Key')
            {
                targetLocation '/home/camunda/.ssh/id_rsa'
            }
            // nexus settings xml
            mavenSettings(mavenSettingsId)
            {
                variable('NEXUS_SETTINGS')
            }
        }

        credentialsBinding {
          // maven central signing credentials
          string('GPG_PASSPHRASE', 'password_maven_central_gpg_signing_key')
          file('MVN_CENTRAL_GPG_KEY_SEC', 'maven_central_gpg_signing_key')
          file('MVN_CENTRAL_GPG_KEY_PUB', 'maven_central_gpg_signing_key_pub')
          // github token for release upload
          string('GITHUB_TOKEN', 'github-camunda-jenkins-token')
        }

        release
        {
            doNotKeepLog false
            overrideBuildParameters true

            parameters
            {
                stringParam('RELEASE_VERSION', '0.1.0', 'Version to release')
                stringParam('DEVELOPMENT_VERSION', '0.2.0-SNAPSHOT', 'Next development version')
                booleanParam('PUSH_CHANGES', true, 'If <strong>TRUE</strong>, push the changes to remote repositories.  If <strong>FALSE</strong>, do not push changes to remote repositories. Must be used in conjunction with USE_LOCAL_CHECKOUT = <strong>TRUE</strong> to test the release!')
                booleanParam('USE_LOCAL_CHECKOUT', false, 'If <strong>TRUE</strong>, uses the local git repository to checkout the release tag to build.  If <strong>FALSE</strong>, checks out the release tag from the remote repositoriy. Must be used in conjunction with PUSH_CHANGES = <strong>FALSE</strong> to test the release!')
                booleanParam('SKIP_DEPLOY_TO_MAVEN_CENTRAL', false, 'If <strong>TRUE</strong>, skip the deployment to maven central. Should be used when testing the release.')
                booleanParam('SKIP_DEPLOY_TO_CAMUNDA_NEXUS', false, 'If <strong>TRUE</strong>, skip the deployment to camunda nexus. Should be used when testing the release.')
            }

            preBuildSteps
            {
                // setup git configuration to push to github
                shell setupGitConfig
                shell mavenGpgKeys

                // execute maven release
                maven
                {
                    mavenInstallation mavenVersion
                    providedSettings mavenSettingsId
                    goals 'release:prepare release:perform -Dgpg.passphrase="${GPG_PASSPHRASE}" -B'

                    properties releaseProperties
                    localRepository LocalRepositoryLocation.LOCAL_TO_WORKSPACE
                }

                shell githubRelease

            }

        }

    }

    publishers
    {

        archiveJunit('**/target/surefire-reports/*.xml')
        {
            retainLongStdout()
        }

        extendedEmail
        {
          triggers
          {
              firstFailure
              {
                  sendTo
                  {
                      culprits()
                  }
              }
              fixed
              {
                  sendTo
                  {
                      culprits()
                  }
              }
          }
        }
    }

    logRotator(-1, 5, -1, 1)

}
