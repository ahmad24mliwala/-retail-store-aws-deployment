def call(Map config) {

    if (!env.IMAGE?.trim()) {
        error "Docker image name is not set. detectVersion must run first."
    }

    echo "=============================================="
    echo "Building Docker Image"
    echo "Service : ${config.service}"
    echo "Image   : ${env.IMAGE}"
    echo "=============================================="

    /*
     * Build Linux AMD64 image.
     *
     * EKS worker nodes in this project use x86_64/AMD64.
     * Explicit platform selection also prevents architecture
     * problems when builds run from ARM-based environments.
     */
    sh """
        docker build \
          --platform linux/amd64 \
          -t ${env.IMAGE} \
          src/${config.service}
    """

    /*
     * Authenticate securely with Docker Hub.
     * Jenkins injects credentials temporarily.
     */
    withCredentials([
        usernamePassword(
            credentialsId: 'dockerhub-creds',
            usernameVariable: 'DOCKERHUB_USER',
            passwordVariable: 'DOCKERHUB_PASS'
        )
    ]) {

        sh '''
            echo "$DOCKERHUB_PASS" | \
            docker login \
              --username "$DOCKERHUB_USER" \
              --password-stdin
        '''

        echo "Pushing ${env.IMAGE}"

        sh """
            docker push ${env.IMAGE}
        """
    }

    echo "=============================================="
    echo "Docker Image Published Successfully"
    echo "Image: ${env.IMAGE}"
    echo "=============================================="
}
