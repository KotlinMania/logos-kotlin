#if canImport(Testing)
import Testing
import Logos

@Suite("Logos Export Tests")
struct LogosExportTests {
    @Test("Swift module loads and imports cleanly")
    func swiftModuleLoads() {
        #expect(Bool(true))
    }
}
#else
import XCTest
import Logos

final class LogosExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Logos swift module imported cleanly")
    }
}
#endif
