#!/usr/bin/env python3
"""Generate ColaPri paper figures (PDF, vector) from the final Fase A measurements.
Numbers are the mean +/- CI95 reported in RESULTADOS_faseA.md. Run: python make_figures.py"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

OUT = os.path.join(os.path.dirname(__file__), "figures")
os.makedirs(OUT, exist_ok=True)
plt.rcParams.update({"font.size": 11, "figure.autolayout": True})
GREEN, AMBER, GREY, BLUE = "#2e8b57", "#e0a106", "#888888", "#3b6ea5"


def fig_starvation():
    sched = ["FCFS", "SJF", "Strict", "Aging", "WFQ", "ColaPri"]
    wait = [465.2, 680.4, 721.0, 519.6, 8.2, 239.5]
    err = [10.9, 22.5, 10.0, 4.3, 1.3, 46.9]
    colors = [GREY, GREY, AMBER, AMBER, BLUE, GREEN]
    fig, ax = plt.subplots(figsize=(6, 3.4))
    ax.bar(sched, wait, yerr=err, capsize=4, color=colors)
    ax.set_ylabel("max waiting, low class p9 (s)")
    ax.set_title("Starvation under sustained load (lower is better)")
    ax.annotate("-67% vs Strict", xy=(5, 239.5), xytext=(3.4, 430),
                arrowprops=dict(arrowstyle="->", color=GREEN), color=GREEN, fontweight="bold")
    fig.savefig(os.path.join(OUT, "fig_starvation.pdf"))
    plt.close(fig)


def fig_weights():
    w = [1, 2, 4, 6, 10, 20]
    p0 = [114.8, 90.2, 76.4, 71.8, 68.2, 65.5]
    p9 = [116.7, 141.4, 155.5, 160.3, 164.2, 166.8]
    ratio = [1.0, 1.6, 2.1, 2.3, 2.4, 2.6]
    fig, ax1 = plt.subplots(figsize=(6, 3.4))
    ax1.plot(w, p0, "o-", color=GREEN, label="resp p0 (high)")
    ax1.plot(w, p9, "s-", color=AMBER, label="resp p9 (low)")
    ax1.set_xlabel("configured weight ratio (class 0 : class 9)")
    ax1.set_ylabel("mean response (s)")
    ax1.set_xscale("log"); ax1.set_xticks(w); ax1.set_xticklabels([f"{x}:1" for x in w])
    ax2 = ax1.twinx()
    ax2.plot(w, ratio, "^--", color=BLUE, label="ratio p9/p0")
    ax2.set_ylabel("differentiation ratio p9/p0", color=BLUE)
    ax2.tick_params(axis="y", colors=BLUE)
    ax1.set_title("Differentiation is a tunable knob (D1)")
    lines = ax1.get_lines() + ax2.get_lines()
    ax1.legend(lines, [l.get_label() for l in lines], loc="center right", fontsize=9)
    fig.savefig(os.path.join(OUT, "fig_weights.pdf"))
    plt.close(fig)


def fig_perclass():
    sched = ["FCFS", "SJF", "Strict", "WFQ", "ColaPri"]
    p0 = [114.4, 106.7, 63.4, 141.4, 106.3]
    p5 = [118.3, 105.9, 155.7, 101.4, 117.0]
    p9 = [124.5, 108.9, 210.6, 61.9, 139.5]
    x = np.arange(len(sched)); ww = 0.27
    fig, ax = plt.subplots(figsize=(6.2, 3.4))
    ax.bar(x - ww, p0, ww, label="p0 (high)", color=GREEN)
    ax.bar(x, p5, ww, label="p5 (mid)", color=AMBER)
    ax.bar(x + ww, p9, ww, label="p9 (low)", color=GREY)
    ax.set_xticks(x); ax.set_xticklabels(sched)
    ax.set_ylabel("mean response (s)")
    ax.set_title("Per-class response: flat / extreme / graded")
    ax.legend(fontsize=9)
    fig.savefig(os.path.join(OUT, "fig_perclass.pdf"))
    plt.close(fig)


def fig_traces():
    ds = ["PlanetLab", "Alibaba", "Bitbrains", "Materna", "Azure"]
    fcfs = [40.7, 232.0, 185.5, 35.5, 40.9]
    cola = [21.0, 127.7, 108.6, 16.6, 21.8]
    strict = [10.0, 71.4, 64.7, 6.2, 10.4]
    x = np.arange(len(ds)); ww = 0.27
    fig, ax = plt.subplots(figsize=(6.4, 3.4))
    ax.bar(x - ww, fcfs, ww, label="FCFS", color=GREY)
    ax.bar(x, cola, ww, label="ColaPri", color=GREEN)
    ax.bar(x + ww, strict, ww, label="Strict (lower bound)", color=AMBER)
    ax.set_xticks(x); ax.set_xticklabels(ds, rotation=15)
    ax.set_ylabel("interactive-class response p0 (s)")
    ax.set_title("Real traces: ColaPri speeds the interactive class (-41..-53% vs FCFS)")
    ax.legend(fontsize=9)
    fig.savefig(os.path.join(OUT, "fig_traces.pdf"))
    plt.close(fig)


if __name__ == "__main__":
    fig_starvation(); fig_weights(); fig_perclass(); fig_traces()
    print("figures written to", OUT)
