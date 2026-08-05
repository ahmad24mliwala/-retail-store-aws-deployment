def call(Map config) {

    dir('.') {

        sh """
        echo "Deploying ${config.service} ${config.version}"

        yq -i '
        .retail-store-${config.service}.image.tag = "${config.version}"
        ' retail-store-helm-chart/values/prod/values-prod.yaml

        git config user.email "jenkins@ci.com"
        git config user.name "Jenkins CI"

        git add retail-store-helm-chart/values/prod/values-prod.yaml

        git commit -m "ci: deploy ${config.service}:${config.version}" || true

        git push origin main
        """
    }

}
