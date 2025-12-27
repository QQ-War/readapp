import SwiftUI

@main
struct ReadAppApp: App {
    @StateObject private var apiService = APIService.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(apiService)
        }
    }
}

import AVFoundation

