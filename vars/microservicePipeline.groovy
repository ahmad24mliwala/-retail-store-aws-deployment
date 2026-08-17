def call(Map config) {

    pipeline {

        agent any

        options {
            timestamps()
            disableConcurrentBuilds()
        }

        stages {

            /*
             * ================================================
             * STAGE 1 — DETECT APPLICATION VERSION
             * ================================================
             */
            stage('Detect Version') {

                steps {

                    detectVersion(
                        service: config.service,
                        type: config.type
                    )
                }
            }


            /*
             * ================================================
             * STAGE 2 — BUILD AND PUSH DOCKER IMAGE
             * ================================================
             */
            stage('Build & Push Image') {

                steps {

                    dockerBuildPush(
                        service: config.service
                    )
                }
            }


            /*
             * ================================================
             * STAGE 3 — UPDATE GITOPS HELM VALUES
             * ================================================
             *
             * Jenkins updates values-prod.yaml.
             *
             * Argo CD will later detect the Git commit and
             * synchronize the desired state to Amazon EKS.
             */
            stage('Update GitOps Repository') {

                steps {

                    deployK8s(
                        service: config.service,
                        version: env.VERSION,
                        environment: config.environment ?: 'prod'
                    )
                }
            }
        }


        /*
         * ================================================
         * PIPELINE RESULT
         * ================================================
         */
        post {

            success {

                echo "=============================================="
                echo "CI Pipeline Successful"
                echo "Service : ${config.service}"
                echo "Version : ${env.VERSION}"
                echo "Image   : ${env.IMAGE}"
                echo "=============================================="
            }

            failure {

                echo "=============================================="
                echo "CI Pipeline Failed"
                echo "Service : ${config.service}"
                echo "=============================================="
            }
        }
    }
}
