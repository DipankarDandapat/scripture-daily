package com.scripturedaily

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceUpdateManagerTest {
    @Test fun newerPatchRequiresUpdate() = assertTrue(ForceUpdateManager.isUpdateRequired("1.3.2", "1.3.3"))
    @Test fun newerMinorRequiresUpdate() = assertTrue(ForceUpdateManager.isUpdateRequired("1.3.2", "1.4.0"))
    @Test fun newerMajorRequiresUpdate() = assertTrue(ForceUpdateManager.isUpdateRequired("1.3.2", "2.0.0"))
    @Test fun numericNotLexicalCompare() = assertTrue(ForceUpdateManager.isUpdateRequired("1.9.0", "1.10.0"))
    @Test fun sameVersionDoesNotRequireUpdate() = assertFalse(ForceUpdateManager.isUpdateRequired("1.3.2", "1.3.2"))
    @Test fun olderRemoteDoesNotRequireUpdate() = assertFalse(ForceUpdateManager.isUpdateRequired("1.3.2", "1.3.1"))
    @Test fun missingSegmentsTreatedAsZero() = assertFalse(ForceUpdateManager.isUpdateRequired("1.3.0", "1.3"))
    @Test fun emptyRemoteDoesNotRequireUpdate() = assertFalse(ForceUpdateManager.isUpdateRequired("1.3.2", ""))
    @Test fun blankMinKeepsOldBehaviourAndForces() = assertTrue(ForceUpdateManager.isForced("1.3.2", "1.4.0", ""))
    @Test fun belowMinIsForced() = assertTrue(ForceUpdateManager.isForced("1.3.2", "1.5.0", "1.4.0"))
    @Test fun aboveMinButBelowLatestIsNotForced() = assertFalse(ForceUpdateManager.isForced("1.4.1", "1.5.0", "1.4.0"))
}
