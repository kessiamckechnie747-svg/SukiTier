/**
 * SukiTier Dashboard Utilities & Enhancements
 * Performance optimization, caching, and utility functions
 */

// ============================================================================
// PERFORMANCE MONITORING
// ============================================================================

class PerformanceMonitor {
    constructor() {
        this.metrics = {
            pageLoadTime: 0,
            apiResponseTimes: [],
            embeddingGenerationTime: 0,
            renderTimes: []
        };
        this.startTime = performance.now();
    }

    recordMetric(name, duration) {
        if (Array.isArray(this.metrics[name])) {
            this.metrics[name].push(duration);
        } else {
            this.metrics[name] = duration;
        }
    }

    getAverageResponseTime() {
        if (this.metrics.apiResponseTimes.length === 0) return 0;
        const sum = this.metrics.apiResponseTimes.reduce((a, b) => a + b, 0);
        return sum / this.metrics.apiResponseTimes.length;
    }

    getPageLoadTime() {
        return performance.now() - this.startTime;
    }

    getReport() {
        return {
            pageLoadTime: this.getPageLoadTime().toFixed(2),
            avgApiResponse: this.getAverageResponseTime().toFixed(2),
            totalApiCalls: this.metrics.apiResponseTimes.length,
            averageRenderTime: this.metrics.renderTimes.length > 0 ?
                (this.metrics.renderTimes.reduce((a, b) => a + b, 0) / this.metrics.renderTimes.length).toFixed(2) : 0
        };
    }
}

// ============================================================================
// ADVANCED CACHING SYSTEM
// ============================================================================

class CacheManager {
    constructor(maxSize = 100, ttl = 3600000) {
        this.cache = new Map();
        this.maxSize = maxSize;
        this.ttl = ttl; // Time-to-live in milliseconds
        this.hits = 0;
        this.misses = 0;
    }

    set(key, value) {
        // Implement LRU eviction
        if (this.cache.size >= this.maxSize) {
            const firstKey = this.cache.keys().next().value;
            this.cache.delete(firstKey);
        }

        this.cache.set(key, {
            value,
            timestamp: Date.now(),
            expiry: Date.now() + this.ttl
        });
    }

    get(key) {
        const entry = this.cache.get(key);

        if (!entry) {
            this.misses++;
            return null;
        }

        // Check if expired
        if (entry.expiry < Date.now()) {
            this.cache.delete(key);
            this.misses++;
            return null;
        }

        this.hits++;
        return entry.value;
    }

    has(key) {
        const entry = this.cache.get(key);
        if (!entry) return false;
        if (entry.expiry < Date.now()) {
            this.cache.delete(key);
            return false;
        }
        return true;
    }

    clear() {
        this.cache.clear();
        this.hits = 0;
        this.misses = 0;
    }

    getStats() {
        const total = this.hits + this.misses;
        return {
            size: this.cache.size,
            hits: this.hits,
            misses: this.misses,
            hitRate: total === 0 ? 0 : ((this.hits / total) * 100).toFixed(2),
            avgAge: this.getAverageAge()
        };
    }

    getAverageAge() {
        if (this.cache.size === 0) return 0;
        const now = Date.now();
        let totalAge = 0;
        let count = 0;

        for (const entry of this.cache.values()) {
            totalAge += (now - entry.timestamp);
            count++;
        }

        return (totalAge / count).toFixed(0);
    }
}

// ============================================================================
// ENHANCED MATHEMATICAL ENGINE
// ============================================================================

class AdvancedMathEngine {
    static normalize(vector) {
        const magnitude = Math.sqrt(
            vector.reduce((sum, val) => sum + val * val, 0)
        );
        return magnitude === 0 ? vector : vector.map(v => v / magnitude);
    }

    static dotProduct(vecA, vecB) {
        if (vecA.length !== vecB.length) throw new Error('Vector dimension mismatch');
        return vecA.reduce((sum, val, i) => sum + val * vecB[i], 0);
    }

    static cosineSimilarity(vecA, vecB) {
        const normA = this.normalize(vecA);
        const normB = this.normalize(vecB);
        return this.dotProduct(normA, normB);
    }

    static euclideanDistance(vecA, vecB) {
        if (vecA.length !== vecB.length) throw new Error('Vector dimension mismatch');
        return Math.sqrt(
            vecA.reduce((sum, val, i) => sum + Math.pow(val - vecB[i], 2), 0)
        );
    }

    static manhattanDistance(vecA, vecB) {
        if (vecA.length !== vecB.length) throw new Error('Vector dimension mismatch');
        return vecA.reduce((sum, val, i) => sum + Math.abs(val - vecB[i]), 0);
    }

    static chebyshevDistance(vecA, vecB) {
        if (vecA.length !== vecB.length) throw new Error('Vector dimension mismatch');
        return Math.max(...vecA.map((val, i) => Math.abs(val - vecB[i])));
    }

    static minkowskiDistance(vecA, vecB, p = 2) {
        if (vecA.length !== vecB.length) throw new Error('Vector dimension mismatch');
        const sum = vecA.reduce((acc, val, i) => acc + Math.pow(Math.abs(val - vecB[i]), p), 0);
        return Math.pow(sum, 1 / p);
    }

    static pca(vectors, k = 2) {
        // Simplified PCA - center data
        const mean = this.getMean(vectors);
        const centered = vectors.map(v =>
            v.map((val, i) => val - mean[i])
        );

        // Return principal components (simplified)
        return centered.slice(0, k);
    }

    static getMean(vectors) {
        if (vectors.length === 0) return [];
        const dimension = vectors[0].length;
        const mean = new Array(dimension).fill(0);

        for (let d = 0; d < dimension; d++) {
            let sum = 0;
            for (let i = 0; i < vectors.length; i++) {
                sum += vectors[i][d];
            }
            mean[d] = sum / vectors.length;
        }

        return mean;
    }

    static entropy(vector) {
        // Calculate Shannon entropy
        const sum = vector.reduce((a, b) => a + b, 0);
        const probabilities = vector.map(v => v / sum);
        return -probabilities.reduce((sum, p) => {
            return sum + (p > 0 ? p * Math.log2(p) : 0);
        }, 0);
    }
}

// ============================================================================
// ADVANCED STATE MANAGEMENT
// ============================================================================

class StateManager {
    constructor() {
        this.state = {
            currentTab: 'engine',
            embeddings: [],
            rules: new Map(),
            verificationResults: [],
            metrics: {}
        };
        this.listeners = [];
        this.history = [];
        this.historyIndex = -1;
    }

    setState(updates) {
        const oldState = JSON.parse(JSON.stringify(this.state));
        this.state = { ...this.state, ...updates };

        // Add to history for undo/redo
        this.history = this.history.slice(0, this.historyIndex + 1);
        this.history.push(oldState);
        this.historyIndex++;

        // Notify listeners
        this.listeners.forEach(listener => listener(this.state));
    }

    subscribe(listener) {
        this.listeners.push(listener);
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }

    undo() {
        if (this.historyIndex > 0) {
            this.historyIndex--;
            this.state = JSON.parse(JSON.stringify(this.history[this.historyIndex]));
            this.listeners.forEach(listener => listener(this.state));
        }
    }

    redo() {
        if (this.historyIndex < this.history.length - 1) {
            this.historyIndex++;
            this.state = JSON.parse(JSON.stringify(this.history[this.historyIndex]));
            this.listeners.forEach(listener => listener(this.state));
        }
    }

    getState() {
        return this.state;
    }
}

// ============================================================================
// ERROR HANDLING & RETRY LOGIC
// ============================================================================

class ResilientAPIClient {
    constructor(maxRetries = 3, baseDelay = 1000) {
        this.maxRetries = maxRetries;
        this.baseDelay = baseDelay;
    }

    async request(url, options = {}) {
        let lastError;

        for (let attempt = 0; attempt < this.maxRetries; attempt++) {
            try {
                const response = await axios(url, options);
                return response.data;
            } catch (error) {
                lastError = error;
                console.warn(`Attempt ${attempt + 1} failed for ${url}`, error.message);

                if (attempt < this.maxRetries - 1) {
                    const delay = this.baseDelay * Math.pow(2, attempt); // Exponential backoff
                    await new Promise(resolve => setTimeout(resolve, delay));
                }
            }
        }

        throw new Error(`Failed after ${this.maxRetries} attempts: ${lastError.message}`);
    }

    async requestWithTimeout(url, timeout = 5000, options = {}) {
        return Promise.race([
            this.request(url, options),
            new Promise((_, reject) =>
                setTimeout(() => reject(new Error('Request timeout')), timeout)
            )
        ]);
    }
}

// ============================================================================
// VISUALIZATION UTILITIES
// ============================================================================

class VisualizationHelper {
    static createScatterPlot(containerId, data, options = {}) {
        const canvas = document.getElementById(containerId);
        if (!canvas) return;

        canvas.innerHTML = '';
        const width = canvas.offsetWidth;
        const height = canvas.offsetHeight;

        const minX = Math.min(...data.map(d => d.x));
        const maxX = Math.max(...data.map(d => d.x));
        const minY = Math.min(...data.map(d => d.y));
        const maxY = Math.max(...data.map(d => d.y));

        data.forEach(point => {
            const px = ((point.x - minX) / (maxX - minX)) * width;
            const py = ((point.y - minY) / (maxY - minY)) * height;

            const element = document.createElement('div');
            element.className = 'absolute w-2 h-2 rounded-full';
            element.style.left = `${px}px`;
            element.style.top = `${py}px`;
            element.style.backgroundColor = point.color || '#3b82f6';
            element.style.boxShadow = `0 0 5px ${point.color || '#3b82f6'}`;

            canvas.appendChild(element);
        });
    }

    static createHistogram(containerId, data, bins = 20) {
        const canvas = document.getElementById(containerId);
        if (!canvas) return;

        const min = Math.min(...data);
        const max = Math.max(...data);
        const binSize = (max - min) / bins;
        const histogram = new Array(bins).fill(0);

        data.forEach(value => {
            const binIndex = Math.min(
                Math.floor((value - min) / binSize),
                bins - 1
            );
            histogram[binIndex]++;
        });

        // Render histogram
        canvas.innerHTML = '';
        const maxCount = Math.max(...histogram);

        histogram.forEach((count, index) => {
            const bar = document.createElement('div');
            const height = (count / maxCount) * 100;
            bar.style.cssText = `
                display: inline-block;
                width: ${100 / bins}%;
                height: ${height}%;
                background: #3b82f6;
                border-right: 1px solid #111;
            `;
            canvas.appendChild(bar);
        });
    }
}

// ============================================================================
// EXPORT FOR USE
// ============================================================================

if (typeof window !== 'undefined') {
    window.PerformanceMonitor = PerformanceMonitor;
    window.CacheManager = CacheManager;
    window.AdvancedMathEngine = AdvancedMathEngine;
    window.StateManager = StateManager;
    window.ResilientAPIClient = ResilientAPIClient;
    window.VisualizationHelper = VisualizationHelper;
}

// Initialize global instances
const performanceMonitor = new PerformanceMonitor();
const cacheManager = new CacheManager();
const stateManager = new StateManager();
const apiClient = new ResilientAPIClient();
