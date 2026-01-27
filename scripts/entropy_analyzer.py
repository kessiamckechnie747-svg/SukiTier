#!/usr/bin/env python3
"""
Debug Log Entropy Spike Parser
Highlights Shannon Entropy spikes in neon warning color (#FF0055)
DCS-style Distributed Control System display
"""

import sys
import re
import argparse
from pathlib import Path
from typing import List, Tuple

class AnsiColors:
    """ANSI color codes for terminal output"""
    RESET = '\033[0m'
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    MAGENTA = '\033[0;35m'
    CYAN = '\033[0;36m'
    
    # Neon warning
    NEON_WARNING = '\033[38;5;198m'  # Bright magenta (#FF0055)
    BRIGHT_GREEN = '\033[38;5;46m'   # Terminal green (#00FF00)
    
    BOLD = '\033[1m'
    DIM = '\033[2m'

class EntropyParser:
    """Parse debug logs for entropy analysis"""
    
    # Regex patterns to detect entropy values
    ENTROPY_PATTERN = re.compile(
        r'entropy[:\s=]+(\d+\.\d+|[0-9]+)',
        re.IGNORECASE
    )
    
    BINARY_PATTERN = re.compile(
        r'(?:file|binary|module)[:\s]+([^\s]+(?:\.ko|\.so|\.bin|\.elf))',
        re.IGNORECASE
    )
    
    def __init__(self, threshold: float = 7.8):
        self.threshold = threshold
        self.findings = []
    
    def parse_log_file(self, file_path: str) -> List[Tuple[int, str, float]]:
        """
        Parse log file and extract entropy spikes
        Returns: List of (line_number, context, entropy_value)
        """
        spike_lines = []
        
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                for line_num, line in enumerate(f, 1):
                    # Look for entropy values
                    entropy_match = self.ENTROPY_PATTERN.search(line)
                    if entropy_match:
                        try:
                            entropy_value = float(entropy_match.group(1))
                            
                            # Check for spike
                            if entropy_value > self.threshold:
                                spike_lines.append((line_num, line.strip(), entropy_value))
                        except ValueError:
                            pass
            
            return spike_lines
        
        except FileNotFoundError:
            print(f"{AnsiColors.RED}Error: File not found: {file_path}{AnsiColors.RESET}")
            return []
        except Exception as e:
            print(f"{AnsiColors.RED}Error reading file: {e}{AnsiColors.RESET}")
            return []
    
    def display_dcs_report(self, spikes: List[Tuple[int, str, float]]):
        """Display DCS-style report of entropy spikes"""
        
        print(f"\n{AnsiColors.BOLD}{AnsiColors.BRIGHT_GREEN}{'='*70}{AnsiColors.RESET}")
        print(f"{AnsiColors.BRIGHT_GREEN}DISTRIBUTED CONTROL SYSTEM - ENTROPY ANALYSIS REPORT{AnsiColors.RESET}")
        print(f"{AnsiColors.BOLD}{AnsiColors.BRIGHT_GREEN}{'='*70}{AnsiColors.RESET}\n")
        
        if not spikes:
            print(f"{AnsiColors.GREEN}✓ No entropy spikes detected above threshold {self.threshold}{AnsiColors.RESET}")
            print(f"{AnsiColors.DIM}System status: NOMINAL{AnsiColors.RESET}\n")
            return
        
        print(f"{AnsiColors.NEON_WARNING}{AnsiColors.BOLD}⚠ ENTROPY SPIKE ALERT{AnsiColors.RESET}")
        print(f"{AnsiColors.DIM}Found {len(spikes)} potential high-entropy anomalies{AnsiColors.RESET}\n")
        
        for line_num, line_content, entropy_value in spikes:
            confidence = self._calculate_confidence(entropy_value)
            
            # Format output
            status = "CRITICAL" if entropy_value >= 7.9 else "WARNING"
            status_color = AnsiColors.RED if entropy_value >= 7.9 else AnsiColors.NEON_WARNING
            
            print(f"{AnsiColors.BOLD}{status_color}[{status}]{AnsiColors.RESET} Line {line_num:6d} | Entropy: {entropy_value:.4f} (conf: {confidence:.0%})")
            print(f"  {AnsiColors.DIM}{line_content[:100]}{AnsiColors.RESET}")
            print()
        
        # Summary
        print(f"{AnsiColors.BOLD}{AnsiColors.BRIGHT_GREEN}{'='*70}{AnsiColors.RESET}")
        print(f"{AnsiColors.BOLD}SUMMARY{AnsiColors.RESET}")
        print(f"  Total spikes: {len(spikes)}")
        print(f"  Threshold: {self.threshold}")
        print(f"  Range: {min(s[2] for s in spikes):.4f} - {max(s[2] for s in spikes):.4f}")
        print(f"{AnsiColors.BOLD}{AnsiColors.BRIGHT_GREEN}{'='*70}{AnsiColors.RESET}\n")
    
    @staticmethod
    def _calculate_confidence(entropy_value: float) -> float:
        """Calculate confidence that binary is packed/obfuscated"""
        if entropy_value >= 7.9:
            return 0.95
        elif entropy_value >= 7.8:
            return 0.85
        elif entropy_value >= 7.5:
            return 0.50
        else:
            return 0.10
    
    def export_csv(self, spikes: List[Tuple[int, str, float]], output_file: str):
        """Export findings to CSV"""
        try:
            import csv
            with open(output_file, 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerow(['Line Number', 'Entropy Value', 'Context'])
                for line_num, context, entropy in spikes:
                    writer.writerow([line_num, entropy, context[:200]])
            print(f"{AnsiColors.GREEN}✓ Results exported to {output_file}{AnsiColors.RESET}")
        except Exception as e:
            print(f"{AnsiColors.RED}Error exporting CSV: {e}{AnsiColors.RESET}")

def main():
    parser = argparse.ArgumentParser(
        description='Parse debug logs for Shannon entropy spikes',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --input debug.log --threshold 7.8
  %(prog)s --input app.log --export results.csv
        """
    )
    
    parser.add_argument(
        '--input', '-i',
        required=True,
        help='Path to debug log file'
    )
    
    parser.add_argument(
        '--threshold', '-t',
        type=float,
        default=7.8,
        help='Entropy threshold for spike detection (default: 7.8)'
    )
    
    parser.add_argument(
        '--export', '-e',
        help='Export results to CSV file'
    )
    
    args = parser.parse_args()
    
    # Create parser
    entropy_parser = EntropyParser(threshold=args.threshold)
    
    # Parse log file
    spikes = entropy_parser.parse_log_file(args.input)
    
    # Display results
    entropy_parser.display_dcs_report(spikes)
    
    # Export if requested
    if args.export:
        entropy_parser.export_csv(spikes, args.export)
    
    # Exit with appropriate code
    sys.exit(0 if not spikes else 1)

if __name__ == '__main__':
    main()
