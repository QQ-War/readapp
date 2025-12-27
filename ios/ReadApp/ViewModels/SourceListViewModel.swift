import Foundation
import Combine

class SourceListViewModel: ObservableObject {
    @Published var sources: [BookSource] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    @MainActor
    func fetchSources() {
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let fetchedSources = try await APIService.shared.fetchBookSources()
                self.sources = fetchedSources
            } catch {
                self.errorMessage = "加载书源失败: \(error.localizedDescription)"
            }
            isLoading = false
        }
    }
    
    @MainActor
    func deleteSource(source: BookSource) {
        Task {
            do {
                try await APIService.shared.deleteBookSource(id: source.bookSourceUrl)
                if let index = sources.firstIndex(where: { $0.id == source.id }) {
                    sources.remove(at: index)
                }
            } catch {
                self.errorMessage = "删除失败: \(error.localizedDescription)"
            }
        }
    }
    
    @MainActor
    func toggleSource(source: BookSource) {
        guard let index = sources.firstIndex(where: { $0.id == source.id }) else { return }
        let newState = !source.enabled
        
        // Optimistic update
        sources[index] = BookSource(
            bookSourceName: source.bookSourceName,
            bookSourceGroup: source.bookSourceGroup,
            bookSourceUrl: source.bookSourceUrl,
            bookSourceType: source.bookSourceType,
            customOrder: source.customOrder,
            enabled: newState,
            enabledExplore: source.enabledExplore,
            lastUpdateTime: source.lastUpdateTime,
            weight: source.weight,
            bookSourceComment: source.bookSourceComment,
            respondTime: source.respondTime
        )
        
        Task {
            do {
                try await APIService.shared.toggleBookSource(id: source.bookSourceUrl, isEnabled: newState)
            } catch {
                // Revert on failure
                if let idx = sources.firstIndex(where: { $0.id == source.id }) {
                    sources[idx] = source // Revert to original object
                }
                self.errorMessage = "操作失败: \(error.localizedDescription)"
            }
        }
    }
}
