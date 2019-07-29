pipeline {
  agent {
    label 'docker'
  }
  options {
    ansiColor('xterm')
  }
  stages {
    stage("Init title") {
      when { changeRequest() }
      steps {
        script {
          currentBuild.displayName = "PR ${env.CHANGE_ID}: ${env.CHANGE_TITLE}"
        }
      }
    }
    stage('Build and Package K') {
      failFast true
      parallel {
        stage('Build and Package K on Linux') {
          stages {
            stage('Build and Package on Ubuntu Bionic') {
              stages {
                stage('Build on Ubuntu Bionic') {
                  agent {
                    dockerfile {
                      filename 'Dockerfile.debian'
                      additionalBuildArgs '--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) --build-arg BASE_IMAGE=ubuntu:bionic'
                      reuseNode true
                    }
                  }
                  stages {
                    stage('Checkout code') {
                      steps {
                        dir('k-exercises') {
                          git url: 'git@github.com:kframework/k-exercises.git'
                        }
                      }
                    }
                    stage('Build and Test K') {
                      steps {
                        sh '''
                          echo 'Setting up environment...'
                          eval `opam config env`
                          . $HOME/.cargo/env
                          echo 'Building K...'
                          mvn verify -U
                          echo 'Starting kserver...'
                          k-distribution/target/release/k/bin/spawn-kserver kserver.log
                          cd k-exercises/tutorial
                          make -j`nproc`
                        '''
                      }
                    }
                    stage('Build Debian Package') {
                      steps {
                        dir('kframework-5.0.0') {
                          checkout scm
                          sh '''
                            . $HOME/.cargo/env
                            mv debian/control.ubuntu debian/control
                            dpkg-buildpackage
                          '''
                        }
                        stash name: "bionic", includes: "kframework_5.0.0_amd64.deb"
                      }
                    }
                  }
                  post {
                    always {
                      sh 'k-distribution/target/release/k/bin/stop-kserver || true'
                      archiveArtifacts 'kserver.log,k-distribution/target/kserver.log'
                    }
                  }
                }
                stage('Test Debian Package') {
                  agent {
                    docker {
                      image 'ubuntu:bionic'
                      args '-u 0'
                      reuseNode true
                    }
                  }
                  options { skipDefaultCheckout() }
                  steps {
                    unstash "bionic"
                    sh 'src/main/scripts/test-in-container-debian'
                  }
                  post {
                    always {
                      sh 'stop-kserver || true'
                      archiveArtifacts 'kserver.log,k-distribution/target/kserver.log'
                    }
                  }
                }
              }
            }
            stage('Build and Package on Debian Buster') {
              when {
                anyOf {
                  not { changeRequest() }
                  changelog '.*^\\[build-system\\] .+$'
                  changeset 'Jenkinsfile'
                  changeset 'Dockerfile'
                }
              }
              stages {
                stage('Build on Debian Buster') {
                  agent {
                    dockerfile {
                      filename 'Dockerfile.debian'
                      additionalBuildArgs '--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) --build-arg BASE_IMAGE=debian:buster'
                      reuseNode true
                    }
                  }
                  stages {
                    stage('Build Debian Package') {
                      steps {
                        dir('kframework-5.0.0') {
                          checkout scm
                          sh '''
                            . $HOME/.cargo/env
                            mv debian/control.debian debian/control
                            dpkg-buildpackage
                          '''
                        }
                        stash name: "buster", includes: "kframework_5.0.0_amd64.deb"
                      }
                    }
                  }
                }
                stage('Test Debian Package') {
                  agent {
                    docker {
                      image 'debian:buster'
                      args '-u 0'
                      reuseNode true
                    }
                  }
                  options { skipDefaultCheckout() }
                  steps {
                    unstash "buster"
                    sh '''
                      src/main/scripts/test-in-container-debian
                    '''
                  }
                  post {
                    always {
                      sh 'stop-kserver || true'
                      archiveArtifacts 'kserver.log,k-distribution/target/kserver.log'
                    }
                  }
                }
              }
              post {
                failure {
                  slackSend color: '#cb2431'                                             \
                          , channel: '#k'                                                \
                          , message: "Debian Buster Packaging Failed: ${env.BUILD_URL}"
                }
              }
            }
            stage('Build and Package on Arch Linux') {
              when {
                anyOf {
                  not { changeRequest() }
                  changelog '.*^\\[build-system\\] .+$'
                  changeset 'Jenkinsfile'
                  changeset 'Dockerfile'
                }
              }
              stages {
                stage('Build on Arch Linux') {
                  agent {
                    dockerfile {
                      filename 'Dockerfile.arch'
                      additionalBuildArgs '--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)'
                      reuseNode true
                    }
                  }
                  stages {
                    stage('Build Pacman Package') {
                      steps {
                        checkout scm
                        sh '''
                          makepkg
                        '''
                        stash name: "arch", includes: "kframework-5.0.0-1-x86_64.pkg.tar.xz"
                      }
                    }
                  }
                }
                stage('Test Arch Package') {
                  agent {
                    docker {
                      image 'archlinux/base'
                      args '-u 0'
                      reuseNode true
                    }
                  }
                  options { skipDefaultCheckout() }
                  steps {
                    unstash "arch"
                    sh '''
                      pacman -Syyu --noconfirm
                      pacman -U --noconfirm kframework-5.0.0-1-x86_64.pkg.tar.xz
                      src/main/scripts/test-in-container
                    '''
                  }
                  post {
                    always {
                      sh 'stop-kserver || true'
                      archiveArtifacts 'kserver.log,k-distribution/target/kserver.log'
                    }
                  }
                }
              }
              post {
                failure {
                  slackSend color: '#cb2431'                                         \
                          , channel: '#k'                                            \
                          , message: "Arch Linux Packaging Failed: ${env.BUILD_URL}"
                }
              }
            }
          }
        }
        stage('Build and Package on Mac OS') {
          when {
            anyOf {
              not { changeRequest() }
              changelog '.*^\\[build-system\\] .+$'
              changeset 'Jenkinsfile'
              changeset 'Dockerfile'
            }
          }
          stages {
            stage('Build on Mac OS') {
              agent {
                label 'anka'
              }
              stages {
                stage('Build and Test on Mac OS') {
                  steps {
                    sh '''
                      echo 'Setting up environment...'
                      ./src/main/scripts/brew-install-deps
                      eval `opam config env`
                      . $HOME/.cargo/env
                      echo 'Building K...'
                      mvn verify -U
                    '''
                  }
                }
              }
              post {
                always {
                  archiveArtifacts 'kserver.log,k-distribution/target/kserver.log'
                }
              }
            }
          }
          post {
            failure {
              slackSend color: '#cb2431'                                    \
                      , channel: '#k'                                       \
                      , message: "MacOS Packaging Failed: ${env.BUILD_URL}"
            }
          }
        }
      }
    }
    stage('Deploy') {
      agent {
        dockerfile {
          filename 'Dockerfile.debian'
          additionalBuildArgs '--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) --build-arg BASE_IMAGE=ubuntu:bionic'
          reuseNode true
        }
      }
      when {
        not { changeRequest() }
        branch 'master'
        beforeAgent true
      }
      environment {
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        AWS_REGION='us-east-2'
        GITHUB_TOKEN = credentials('rv-jenkins')
      }
      steps {
        dir("bionic") {
          unstash "bionic"
        }
        dir("buster") {
          unstash "buster"
        }
        dir("arch") {
          unstash "arch"
        }
        dir("kframework-5.0.0") {
          checkout scm
          sh '''
            find . -name .git | xargs rm -r
            cd ..
            tar czvf kframework-5.0.0.tar.gz kframework-5.0.0
          '''
        }
        sh '''
          echo 'Setting up environment...'
          eval `opam config env`
          . $HOME/.cargo/env
          echo 'Deploying K...'
          mvn install -DskipKTest -Dcheckstyle.skip
          COMMIT=$(git rev-parse --short HEAD)
          DESCRIPTION='This is the nightly release of the K framework. To install, download the appropriate binary package and install using your package manager. You can install a debian package via `sudo apt-get install ./kframework_5.0.0_amd64_$ID.deb` for the appropriate version codename $ID. You can install on Arch Linux using `sudo pacman -S ./kframework-5.0.0-1-x86_64.pkg.tar.xz`. If your OS is not supported, you can download and extract the \\"Platform-Independent K binary\\", and follow the instructions in INSTALL.md within the target directory. Note however that this will not support the Haskell or LLVM Backends. On Windows, start by installing [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10) with Ubuntu (or an Ubuntu VM), after which you can install like Ubuntu. K requires gcc and other Linux libraries to run, and building on native Windows, Cygwin, or MINGW is not supported.'
          RESPONSE=`curl --data '{"tag_name": "nightly-'$COMMIT'","name": "Nightly build of K framework at commit '$COMMIT'","body": "'"$DESCRIPTION"'", "draft": true,"prerelease": true}' https://api.github.com/repos/kframework/k/releases?access_token=$GITHUB_TOKEN`
          ID=`echo "$RESPONSE" | grep '"id": [0-9]*,' -o | head -1 | grep '[0-9]*' -o`
          curl --data-binary @kframework-5.0.0.tar.gz -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/gzip" https://uploads.github.com/repos/kframework/k/releases/$ID/assets?'name=kframework-5.0.0.tar.gz&label=Source+tar.gz'
          curl --data-binary @k-distribution/target/k-nightly.tar.gz -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/gzip" https://uploads.github.com/repos/kframework/k/releases/$ID/assets?'name=nightly.tar.gz&label=Platform-Indepdendent+K+binary'
          curl --data-binary @bionic/kframework_5.0.0_amd64.deb -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/vnd.debian.binary-package" https://uploads.github.com/repos/kframework/k/releases/$ID/assets?'name=kframework_5.0.0_amd64_bionic.deb&label=Ubuntu+Bionic+Debian+Package'
          curl --data-binary @buster/kframework_5.0.0_amd64.deb -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/vnd.debian.binary-package" https://uploads.github.com/repos/kframework/k/releases/$ID/assets?'name=kframework_5.0.0_amd64_buster.deb&label=Debian+Buster+Debian+Package'
          curl --data-binary @arch/kframework-5.0.0-1-x86_64.pkg.tar.xz -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/x-xz" https://uploads.github.com/repos/kframework/k/releases/$ID/assets?'name=kframework-5.0.0-1-x86_64.pkg.tar.xz&label=Arch+Linux+Pacman+Package'
          curl -X PATCH --data '{"draft": false}' https://api.github.com/repos/kframework/k/releases/$ID?access_token=$GITHUB_TOKEN
          curl --data '{"state": "success","target_url": "'$BUILD_URL'","description": "Build succeeded."}' https://api.github.com/repos/kframework/k/statuses/$(git rev-parse origin/master)?access_token=$GITHUB_TOKEN
        '''
      }
      post {
        failure {
          slackSend color: '#cb2431'                                 \
                  , channel: '#k'                                    \
                  , message: "Deploy Phase Failed: ${env.BUILD_URL}"
        }
      }
    }
  }
}
