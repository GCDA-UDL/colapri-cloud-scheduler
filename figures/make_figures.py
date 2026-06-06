#!/usr/bin/env python3
"""Generate ColaPri paper figures (PDF, vector) from the final Fase A measurements.
Numbers are the mean +/- CI95 reported in RESULTADOS_faseA.md. Run: python make_figures.py"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.patches import FancyBboxPatch, Rectangle, FancyArrowPatch

OUT = os.path.join(os.path.dirname(__file__), "figures")
os.makedirs(OUT, exist_ok=True)
plt.rcParams.update({"font.size": 11, "figure.autolayout": True})
GREEN, AMBER, GREY, BLUE = "#2e8b57", "#e0a106", "#888888", "#3b6ea5"
RED, DARK = "#c0392b", "#1b2733"


def _box(ax, x, y, w, h, fc, ec="#30363d", lw=1.2, r=0.06):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle=f"round,pad=0.02,rounding_size={r}",
                                fc=fc, ec=ec, lw=lw, zorder=2))


def _arrow(ax, x1, y1, x2, y2, c=DARK, lw=2.0):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>", mutation_scale=16,
                                 color=c, lw=lw, zorder=4))


def _toks(ax, x, y, seq, n=None, s=0.16, gap=0.04):
    # draw a row of small colored squares from a class sequence
    cmap = {0: GREEN, 5: AMBER, 9: GREY}
    for i, c in enumerate(seq if n is None else seq[:n]):
        ax.add_patch(Rectangle((x + i * (s + gap), y), s, s, fc=cmap[c], ec="white", lw=0.4, zorder=3))


def fig_operation():
    """How ColaPri orders the per-VM queue: mixed arrivals -> weighted virtual-time tags -> dispatch."""
    fig, ax = plt.subplots(figsize=(9.2, 4.2))
    ax.set_xlim(0, 10); ax.set_ylim(0, 5); ax.axis("off")

    # arrivals (mixed stream)
    ax.text(1.0, 4.6, "arriving cloudlets\n(mixed priority classes)", ha="center", fontsize=9, color=DARK)
    rng = [0, 9, 0, 5, 0, 0, 9, 5, 0, 0]
    _toks(ax, 0.3, 3.7, rng)
    _arrow(ax, 2.2, 3.78, 3.0, 3.78)

    # ColaPri box
    _box(ax, 3.0, 1.0, 3.8, 3.6, "#f5f8fa", GREEN, lw=2.0)
    ax.text(4.9, 4.32, "ColaPri  (per-VM CloudletScheduler)", ha="center", fontsize=10,
            color=GREEN, fontweight="bold")
    # adaptive gate
    _box(ax, 3.2, 3.7, 3.4, 0.5, "#eef6ef", GREEN, lw=1.0)
    ax.text(4.9, 3.95, r"adaptive $\alpha\in[0,1]$:  FIFO if backlog $\leq\theta$, else weighted-RR",
            ha="center", fontsize=8, color=DARK)
    # three class lanes with weights
    lanes = [(0, "p0", 6, GREEN, [0, 0, 0, 0, 0, 0]), (5, "p5", 3, AMBER, [5, 5, 5]), (9, "p9", 1, GREY, [9])]
    yl = 3.05
    for cls, lbl, w, col, seq in lanes:
        ax.text(3.35, yl + 0.07, f"{lbl} (w={w})", fontsize=8, color=col, fontweight="bold")
        _toks(ax, 4.25, yl, seq, s=0.18, gap=0.05)
        yl -= 0.52
    ax.text(4.9, 1.28, r"virtual-time tag  $\tau = k / w_c$  (assigned once)", ha="center",
            fontsize=8.5, color=DARK, style="italic")
    _arrow(ax, 6.85, 2.6, 7.6, 2.6)

    # dispatch order
    ax.text(8.7, 4.6, "dispatch order\n($\\propto$ weight, no starvation)", ha="center", fontsize=9, color=DARK)
    disp = [0, 0, 0, 5, 0, 0, 0, 5, 9, 0]
    for i, c in enumerate(disp):
        y = 3.9 - i * 0.32
        _toks(ax, 7.7, y, [c], s=0.2, gap=0)
    _box(ax, 8.5, 0.55, 1.1, 0.55, DARK, DARK)
    ax.text(9.05, 0.82, "PEs", ha="center", color="white", fontsize=9, fontweight="bold")
    _arrow(ax, 7.95, 0.7, 8.5, 0.82)
    fig.savefig(os.path.join(OUT, "fig_operation.pdf"))
    plt.close(fig)


def fig_architecture():
    """Where ColaPri plugs into CloudSim Plus: mapping (broker) is orthogonal to scheduling (per-VM)."""
    fig, ax = plt.subplots(figsize=(9.2, 4.6))
    ax.set_xlim(0, 10); ax.set_ylim(0, 5); ax.axis("off")

    ax.text(5, 4.75, "Tasks (cloudlets) with priority class", ha="center", fontsize=9, color=DARK)
    _toks(ax, 4.0, 4.35, [0, 5, 9, 0, 5, 0], s=0.16, gap=0.05)
    _arrow(ax, 5.0, 4.3, 5.0, 3.95)

    # broker (mapping axis)
    _box(ax, 1.2, 3.2, 7.6, 0.7, "#eef3f8", BLUE, lw=1.6)
    ax.text(5.0, 3.55, "DatacenterBroker — task$\\to$VM mapping", ha="center", fontsize=9.5,
            color=BLUE, fontweight="bold")
    ax.text(5.0, 3.3, "{RoundRobin · Min-Min · Max-Min · Demand-Aware}", ha="center", fontsize=8, color=DARK)

    # VMs (scheduling axis)
    for i, vx in enumerate([1.3, 4.0, 6.7]):
        _arrow(ax, 5.0, 3.18, vx + 1.0, 2.62)
        _box(ax, vx, 1.5, 2.0, 1.1, "#f5f8fa", GREEN, lw=1.6)
        ax.text(vx + 1.0, 2.36, f"VM{i+1}", ha="center", fontsize=9, color=DARK, fontweight="bold")
        ax.text(vx + 1.0, 2.06, "CloudletScheduler", ha="center", fontsize=7.5, color=DARK)
        ax.text(vx + 1.0, 1.84, "= ColaPri", ha="center", fontsize=8.5, color=GREEN, fontweight="bold")
        _toks(ax, vx + 0.35, 1.58, [0, 0, 5, 9], s=0.13, gap=0.04)
        _arrow(ax, vx + 1.0, 1.48, vx + 1.0, 1.12)

    # hosts base
    _box(ax, 1.2, 0.5, 7.6, 0.6, DARK, DARK)
    ax.text(5.0, 0.8, "Hosts  (processing elements, PEs)", ha="center", color="white",
            fontsize=9, fontweight="bold")

    # orthogonality annotation
    ax.text(9.55, 3.55, "mapping\n(which VM)", ha="center", fontsize=8, color=BLUE, rotation=90)
    ax.text(9.55, 2.05, "scheduling\n(order in VM)", ha="center", fontsize=8, color=GREEN, rotation=90)
    fig.savefig(os.path.join(OUT, "fig_architecture.pdf"))
    plt.close(fig)


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


def fig_activation():
    # EXP-F: trade-off resp.p0 (x, lower=faster high class) vs starv.p9 (y, lower=less starvation)
    name = ["always-on", "fixed(15)", "best-fixed(40)", "analytic(20,50)", "fuzzy(20,50)", "learned"]
    p0 = [257.1, 289.2, 295.1, 294.2, 256.9, 294.5]
    st = [55.4, 49.0, 47.5, 46.8, 54.8, 47.8]
    col = [GREY, AMBER, AMBER, GREEN, GREY, BLUE]
    mk = ["o", "s", "s", "*", "o", "^"]
    fig, ax = plt.subplots(figsize=(6.2, 3.6))
    for i in range(len(name)):
        ax.scatter(p0[i], st[i], s=160 if mk[i] == "*" else 90, c=col[i], marker=mk[i],
                   edgecolors="black", linewidths=0.5, zorder=3)
        ax.annotate(name[i], (p0[i], st[i]), textcoords="offset points",
                    xytext=(6, 5), fontsize=8)
    ax.set_xlabel("high-class response p0 (s)  —  lower is better")
    ax.set_ylabel("low-class starvation p9  —  lower is better")
    ax.set_title("EXP-F: activation policies (bottom-left = better on both)")
    ax.grid(True, alpha=0.3)
    fig.savefig(os.path.join(OUT, "fig_activation.pdf"))
    plt.close(fig)


if __name__ == "__main__":
    fig_operation(); fig_architecture()
    fig_starvation(); fig_weights(); fig_perclass(); fig_traces(); fig_activation()
    print("figures written to", OUT)
