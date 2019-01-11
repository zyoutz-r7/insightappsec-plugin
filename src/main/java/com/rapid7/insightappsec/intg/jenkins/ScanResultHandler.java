package com.rapid7.insightappsec.intg.jenkins;

import com.rapid7.insightappsec.intg.jenkins.exception.VulnerabilitySearchException;
import hudson.model.Run;
import org.apache.commons.collections.CollectionUtils;

public class ScanResultHandler {

    public void handleScanResults(Run<?,?> run,
                                  InsightAppSecLogger logger,
                                  BuildAdvanceIndicator buildAdvanceIndicator,
                                  ScanResults scanResults,
                                  boolean enableScanResults) {
        if (enableScanResults) {
            run.addAction(new InsightAppSecScanStepAction(scanResults));
        }

        if (buildAdvanceIndicator.equals(BuildAdvanceIndicator.VULNERABILITY_RESULTS) &&
            !CollectionUtils.isEmpty(scanResults.getVulnerabilities())) {
            logger.log(String.format("Failing build due to %s non-filtered vulnerabilities", scanResults.getVulnerabilities().size()));

            throw new VulnerabilitySearchException();
        }
    }
}