package org.jenkinsci.plugins.insightappsec.exception;

import org.jenkinsci.plugins.insightappsec.BuildAdvanceIndicator;
import org.jenkinsci.plugins.insightappsec.InsightAppSecScanStepAction;
import org.jenkinsci.plugins.insightappsec.ScanResultHandler;
import org.jenkinsci.plugins.insightappsec.ScanResults;
import org.jenkinsci.plugins.insightappsec.InsightAppSecLogger;
import hudson.model.Run;
import org.jenkinsci.plugins.insightappsec.api.vulnerability.VulnerabilityModels;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ScanResultHandlerTest {

    @Mock
    private Run<?,?> run;

    @Mock
    private InsightAppSecLogger logger;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private ScanResultHandler scanResultHandler;

    @Test
    public void handleScanResult_vulnerabilityResultsIndicator_vulnerabilitiesPresent() {
        // given
        ScanResults scanResults = ScanResults.builder()
                                             .vulnerabilities(singletonList(VulnerabilityModels.aCompleteVulnerability().build()))
                                             .build();

        exception.expect(VulnerabilitySearchException.class);
        exception.expectMessage("Non-filtered vulnerabilities were found");

        // when
        scanResultHandler.handleScanResults(run,
                                            logger,
                                            BuildAdvanceIndicator.VULNERABILITY_RESULTS,
                                            scanResults,
                                            true);
    }

    @Test
    public void handleScanResult_vulnerabilityResultsIndicator_noVulnerabilitiesPresent() {
        // given
        ScanResults scanResults = ScanResults.builder().build();

        // when
        scanResultHandler.handleScanResults(run,
                                            logger,
                                            BuildAdvanceIndicator.VULNERABILITY_RESULTS,
                                            scanResults,
                             true);
        // then
        // no exception
        verify(run, times(1)).addAction(any(InsightAppSecScanStepAction.class));
    }

    @Test
    public void handleScanResult_vulnerabilityResultsIndicator_noVulnerabilitiesPresent_notStoreScanResults() {
        // given
        ScanResults scanResults = ScanResults.builder().build();

        // when
        scanResultHandler.handleScanResults(run,
                                            logger,
                                            BuildAdvanceIndicator.VULNERABILITY_RESULTS,
                                            scanResults,
                                            false);
        // then
        // no exception
        verify(run, times(0)).addAction(any(InsightAppSecScanStepAction.class));
    }

    @Test
    public void handleScanResult_nonVulnerabilityResultsIndicator_noVulnerabilitiesPresent() {
        // given
        ScanResults scanResults = ScanResults.builder().build();

        // when
        scanResultHandler.handleScanResults(run,
                                            logger,
                                            BuildAdvanceIndicator.SCAN_COMPLETED,
                                            scanResults,
                             true);
        // then
        // no exception
        verify(run, times(1)).addAction(any(InsightAppSecScanStepAction.class));
    }

}