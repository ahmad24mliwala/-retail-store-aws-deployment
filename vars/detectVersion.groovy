def call(Map config) {

    script {

        def version

        /*
         * =====================================================
         * MAVEN SERVICES
         * cart / orders / ui
         * =====================================================
         */
        if (config.type == 'maven') {

            version = sh(
                script: """
                    cd src/${config.service}
                    ./mvnw help:evaluate \
                      -Dexpression=project.version \
                      -q \
                      -DforceStdout | tail -n 1
                """,
                returnStdout: true
            ).trim()
        }

        /*
         * =====================================================
         * GO SERVICE
         * catalog
         * =====================================================
         */
        else if (config.type == 'go') {

            version = sh(
                script: """
                    cd src/${config.service}
                    grep -oP '@version\\s+\\K[0-9.]+' main.go
                """,
                returnStdout: true
            ).trim()
        }

        /*
         * =====================================================
         * NODE.JS SERVICE
         * checkout
         * =====================================================
         */
        else if (config.type == 'node') {

            def packageJson = readJSON(
                file: "src/${config.service}/package.json"
            )

            version = packageJson.version
        }

        else {
            error "Unsupported build type: ${config.type}"
        }

        /*
         * =====================================================
         * VALIDATION
         * =====================================================
         */

        if (!version?.trim()) {
            error "Unable to detect version for ${config.service}"
        }

        /*
         * =====================================================
         * CI IMAGE INFORMATION
         * =====================================================
         */

        env.VERSION = version

        env.IMAGE = "ahmad2001/retail-store-${config.service}:${version}"

        echo "=============================================="
        echo "Service       : ${config.service}"
        echo "Build Type    : ${config.type}"
        echo "Version       : ${env.VERSION}"
        echo "Docker Image  : ${env.IMAGE}"
        echo "=============================================="
    }
}
