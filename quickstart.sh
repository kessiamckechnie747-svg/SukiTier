#!/bin/bash

# SukiTier Dashboard Quick Start Script
# Automates setup and deployment for local development and production

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DASHBOARD_FILE="$PROJECT_DIR/assets/sukitier-dashboard.html"
ENHANCEMENTS_FILE="$PROJECT_DIR/assets/sukitier-enhancements.js"
DOCKER_COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

# Functions
print_header() {
    echo -e "\n${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ SukiTier Dashboard - Quick Start Script ${NC}${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}\n"
}

print_section() {
    echo -e "\n${YELLOW}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_requirements() {
    print_section "Checking Requirements"
    
    local missing=0
    
    # Check Docker
    if command -v docker &> /dev/null; then
        print_success "Docker installed: $(docker --version)"
    else
        print_error "Docker not found. Install from https://docs.docker.com/get-docker/"
        missing=$((missing + 1))
    fi
    
    # Check Docker Compose
    if command -v docker-compose &> /dev/null; then
        print_success "Docker Compose installed: $(docker-compose --version)"
    else
        print_error "Docker Compose not found. Install from https://docs.docker.com/compose/install/"
        missing=$((missing + 1))
    fi
    
    # Check Node.js (optional)
    if command -v node &> /dev/null; then
        print_success "Node.js installed: $(node --version)"
    else
        print_info "Node.js not found (optional, for alternative deployment)"
    fi
    
    # Check Python (optional)
    if command -v python3 &> /dev/null; then
        print_success "Python 3 installed: $(python3 --version)"
    else
        print_info "Python 3 not found (optional, for alternative deployment)"
    fi
    
    if [ $missing -gt 0 ]; then
        print_error "Missing $missing required component(s)"
        return 1
    fi
    
    print_success "All requirements met"
    return 0
}

verify_files() {
    print_section "Verifying Project Files"
    
    local missing=0
    
    if [ -f "$DASHBOARD_FILE" ]; then
        print_success "Dashboard HTML found"
    else
        print_error "Dashboard HTML not found: $DASHBOARD_FILE"
        missing=$((missing + 1))
    fi
    
    if [ -f "$ENHANCEMENTS_FILE" ]; then
        print_success "Enhancement library found"
    else
        print_error "Enhancement library not found: $ENHANCEMENTS_FILE"
        missing=$((missing + 1))
    fi
    
    if [ -f "$DOCKER_COMPOSE_FILE" ]; then
        print_success "Docker Compose config found"
    else
        print_error "Docker Compose config not found: $DOCKER_COMPOSE_FILE"
        missing=$((missing + 1))
    fi
    
    if [ $missing -gt 0 ]; then
        print_error "Missing $missing file(s)"
        return 1
    fi
    
    print_success "All files verified"
    return 0
}

setup_certificates() {
    print_section "Setting Up SSL Certificates"
    
    local certs_dir="$PROJECT_DIR/certs"
    local cert_file="$certs_dir/cert.pem"
    local key_file="$certs_dir/key.pem"
    
    if [ -f "$cert_file" ] && [ -f "$key_file" ]; then
        print_success "SSL certificates already exist"
        return 0
    fi
    
    print_info "Creating self-signed certificates (development only)"
    
    mkdir -p "$certs_dir"
    
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$key_file" -out "$cert_file" \
        -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost" \
        2>/dev/null
    
    chmod 600 "$key_file"
    chmod 644 "$cert_file"
    
    print_success "SSL certificates created"
    print_info "Note: These are self-signed. For production, use Let's Encrypt or a commercial CA"
}

start_docker() {
    print_section "Starting Docker Containers"
    
    cd "$PROJECT_DIR"
    
    # Stop existing containers
    if docker-compose ps | grep -q Up; then
        print_info "Stopping existing containers..."
        docker-compose down
    fi
    
    # Start containers
    print_info "Building and starting containers..."
    docker-compose up -d
    
    # Wait for containers to be ready
    print_info "Waiting for containers to be ready..."
    sleep 5
    
    # Check status
    if docker-compose ps | grep -q "Up"; then
        print_success "Docker containers started successfully"
        
        # Get container info
        echo -e "\n${BLUE}Container Status:${NC}"
        docker-compose ps
        
        return 0
    else
        print_error "Failed to start Docker containers"
        echo -e "\n${RED}Docker logs:${NC}"
        docker-compose logs
        return 1
    fi
}

start_python() {
    print_section "Starting Python HTTP Server"
    
    local port=8000
    
    if netstat -tuln 2>/dev/null | grep -q ":$port"; then
        print_error "Port $port is already in use"
        return 1
    fi
    
    print_info "Starting server on port $port..."
    cd "$PROJECT_DIR/assets"
    python3 -m http.server $port &
    local pid=$!
    
    echo $pid > "$PROJECT_DIR/.python_server.pid"
    
    sleep 2
    
    if kill -0 $pid 2>/dev/null; then
        print_success "Python HTTP server started (PID: $pid)"
        print_info "Dashboard available at http://localhost:$port"
        return 0
    else
        print_error "Failed to start Python HTTP server"
        return 1
    fi
}

start_nodejs() {
    print_section "Starting Node.js HTTP Server"
    
    if ! command -v http-server &> /dev/null; then
        print_info "Installing http-server..."
        npm install -g http-server
    fi
    
    local port=8000
    
    print_info "Starting server on port $port..."
    cd "$PROJECT_DIR/assets"
    http-server -p $port -c-1 &
    local pid=$!
    
    echo $pid > "$PROJECT_DIR/.nodejs_server.pid"
    
    sleep 2
    
    if kill -0 $pid 2>/dev/null; then
        print_success "Node.js HTTP server started (PID: $pid)"
        print_info "Dashboard available at http://localhost:$port"
        return 0
    else
        print_error "Failed to start Node.js HTTP server"
        return 1
    fi
}

show_menu() {
    print_section "Deployment Options"
    
    echo -e "${BLUE}Choose deployment method:${NC}"
    echo "1) Docker Compose (Recommended - Production Ready)"
    echo "2) Python HTTP Server (Development)"
    echo "3) Node.js HTTP Server (Development)"
    echo "4) Show Configuration"
    echo "5) Stop Containers"
    echo "6) View Logs"
    echo "0) Exit"
    echo ""
    read -p "Enter your choice (0-6): " choice
    
    case $choice in
        1)
            setup_certificates
            start_docker
            show_urls "docker"
            ;;
        2)
            start_python
            show_urls "python"
            ;;
        3)
            start_nodejs
            show_urls "nodejs"
            ;;
        4)
            show_configuration
            show_menu
            ;;
        5)
            print_section "Stopping Containers"
            cd "$PROJECT_DIR"
            docker-compose down
            print_success "Containers stopped"
            show_menu
            ;;
        6)
            print_section "Docker Logs"
            cd "$PROJECT_DIR"
            docker-compose logs -f --tail=50
            ;;
        0)
            print_info "Exiting..."
            exit 0
            ;;
        *)
            print_error "Invalid choice"
            show_menu
            ;;
    esac
}

show_urls() {
    local method=$1
    
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║ Dashboard URLs                         ${NC}${GREEN}║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    
    case $method in
        docker)
            echo -e "${YELLOW}Main:${NC}        https://localhost"
            echo -e "${YELLOW}API:${NC}         https://localhost/api"
            echo -e "${YELLOW}Health:${NC}      https://localhost/health"
            echo -e "${YELLOW}Note:${NC}        Self-signed cert - browser warning is normal"
            ;;
        python|nodejs)
            echo -e "${YELLOW}Dashboard:${NC}   http://localhost:8000"
            echo -e "${YELLOW}Note:${NC}        For development only"
            ;;
    esac
    
    echo ""
    echo -e "${BLUE}Documentation:${NC}"
    echo -e "  • User Guide: $PROJECT_DIR/DASHBOARD_README.md"
    echo -e "  • Deployment: $PROJECT_DIR/DASHBOARD_DEPLOYMENT.md"
    echo -e "  • Summary: $PROJECT_DIR/DASHBOARD_IMPLEMENTATION_SUMMARY.md"
    echo ""
}

show_configuration() {
    print_section "Current Configuration"
    
    echo -e "${YELLOW}Project Directory:${NC} $PROJECT_DIR"
    echo -e "${YELLOW}Dashboard File:${NC} $DASHBOARD_FILE"
    echo -e "${YELLOW}Enhancement File:${NC} $ENHANCEMENTS_FILE"
    echo -e "${YELLOW}Docker Compose:${NC} $DOCKER_COMPOSE_FILE"
    echo ""
}

main() {
    print_header
    
    if ! check_requirements; then
        print_error "Please install missing requirements and try again"
        exit 1
    fi
    
    if ! verify_files; then
        print_error "Please ensure all project files are in place"
        exit 1
    fi
    
    show_menu
}

# Run main function
main
