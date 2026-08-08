def call(Map config) {

    if (!config.service?.trim()) {
        error "Service name is required."
    }

    if (!config.version?.trim()) {
        error "Service version is required."
    }

    def environment = config.environment ?: 'prod'

    def valuesFile =
        "retail-store-helm-chart/values/${environment}/values-${environment}.yaml"

    echo "=============================================="
    echo "Updating GitOps Repository"
    echo "Service     : ${config.service}"
    echo "Version     : ${config.version}"
    echo "Environment : ${environment}"
    echo "Values File : ${valuesFile}"
    echo "=============================================="

    /*
     * Verify that the environment values file exists.
     */
    if (!fileExists(valuesFile)) {
        error "Helm values file not found: ${valuesFile}"
    }

    /*
     * Update only the selected microservice image tag.
     *
     * Example:
     *
     * retail-store-cart:
     *   image:
     *     repository: ahmad2001/retail-store-cart
     *     tag: 1.0.0
     */
    sh """
        yq -i \
          '.["retail-store-${config.service}"].image.tag = "${config.version}"' \
          '${valuesFile}'
    """

    /*
     * Verify the value written by yq.
     */
    def updatedVersion = sh(
        script: """
            yq -r \
              '.["retail-store-${config.service}"].image.tag' \
              '${valuesFile}'
        """,
        returnStdout: true
    ).trim()

    if (updatedVersion != config.version) {
        error "Helm image tag update verification failed."
    }

    echo "Helm image tag updated successfully: ${updatedVersion}"

    /*
     * Commit only when the Helm values file actually changed.
     */
    def hasChanges = sh(
        script: "git diff --quiet -- '${valuesFile}'",
        returnStatus: true
    )

    if (hasChanges == 0) {

        echo "GitOps repository already contains ${config.service}:${config.version}"
        echo "No Git commit required."

        return
    }

    /*
     * Configure Jenkins Git identity.
     */
    sh """
        git config user.email "jenkins@ci.com"
        git config user.name "Jenkins CI"
    """

    /*
     * Stage and commit only the GitOps values file.
     */
    sh """
        git add '${valuesFile}'

        git commit \
          -m "ci: deploy ${config.service}:${config.version} to ${environment}"
    """

    /*
     * Synchronize with remote before pushing.
     *
     * This reduces non-fast-forward failures when another
     * pipeline has updated the GitOps repository.
     */
    sh """
        git pull --rebase origin main
        git push origin HEAD:main
    """

    echo "=============================================="
    echo "GitOps Update Published"
    echo "Service : ${config.service}"
    echo "Version : ${config.version}"
    echo "=============================================="
}
