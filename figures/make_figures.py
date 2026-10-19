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
    ax.text(1.0, 4.62, "(1) arriving cloudlets\n(mixed priority classes, tagged once)", ha="center", fontsize=8.6, color=DARK)
    rng = [0, 9, 0, 5, 0, 0, 9, 5, 0, 0]
    _toks(ax, 0.3, 3.7, rng)
    _arrow(ax, 2.2, 3.78, 3.0, 3.78)

    # ColaPri box
    _box(ax, 3.0, 1.0, 3.8, 3.6, "#f5f8fa", GREEN, lw=2.0)
    ax.text(4.9, 4.32, "ColaPri  (per-VM CloudletScheduler)", ha="center", fontsize=10,
            color=GREEN, fontweight="bold")
    # adaptive gate (two lines so the label never overflows the inner box)
    _box(ax, 3.12, 3.5, 3.56, 0.62, "#eef6ef", GREEN, lw=1.0)
    ax.text(4.9, 3.92, r"adaptive gate  $\alpha\in[0,1]$", ha="center", fontsize=7.8,
            color=DARK, fontweight="bold")
    ax.text(4.9, 3.68, r"FIFO if backlog $\leq\theta$, else weighted-RR", ha="center",
            fontsize=7.0, color=DARK)
    # three class lanes with weights
    lanes = [(0, "p0", 6, GREEN, [0, 0, 0, 0, 0, 0]), (5, "p5", 3, AMBER, [5, 5, 5]), (9, "p9", 1, GREY, [9])]
    yl = 3.05
    for cls, lbl, w, col, seq in lanes:
        ax.text(3.35, yl + 0.07, f"{lbl} (w={w})", fontsize=8, color=col, fontweight="bold")
        _toks(ax, 4.25, yl, seq, s=0.18, gap=0.05)
        yl -= 0.52
    ax.text(4.9, 1.28, r"(2) order by virtual-time tag  $\tau = k / w_c$  (assigned once)", ha="center",
            fontsize=8.0, color=DARK, style="italic")
    _arrow(ax, 6.85, 2.6, 7.6, 2.6)

    # dispatch order
    ax.text(8.7, 4.62, "(3) dispatch order\n($\\propto$ weight, no starvation)", ha="center", fontsize=8.6, color=DARK)
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

    ax.text(5, 4.78, "Tasks (cloudlets) with priority class", ha="center", fontsize=10.5, color=DARK)
    _toks(ax, 4.0, 4.35, [0, 5, 9, 0, 5, 0], s=0.16, gap=0.05)
    _arrow(ax, 5.0, 4.3, 5.0, 3.95)

    # broker (mapping axis)
    _box(ax, 1.2, 3.2, 7.6, 0.7, "#eef3f8", BLUE, lw=1.6)
    ax.text(5.0, 3.55, "DatacenterBroker — task$\\to$VM mapping", ha="center", fontsize=11,
            color=BLUE, fontweight="bold")
    ax.text(5.0, 3.30, "{RoundRobin · Min-Min · Max-Min · Demand-Aware}", ha="center", fontsize=9, color=DARK)

    # VMs (scheduling axis)
    for i, vx in enumerate([1.3, 4.0, 6.7]):
        _arrow(ax, 5.0, 3.18, vx + 1.0, 2.62)
        _box(ax, vx, 1.5, 2.0, 1.1, "#f5f8fa", GREEN, lw=1.6)
        ax.text(vx + 1.0, 2.36, f"VM{i+1}", ha="center", fontsize=10.5, color=DARK, fontweight="bold")
        ax.text(vx + 1.0, 2.04, "CloudletScheduler", ha="center", fontsize=9, color=DARK)
        ax.text(vx + 1.0, 1.80, "= ColaPri", ha="center", fontsize=10, color=GREEN, fontweight="bold")
        _toks(ax, vx + 0.35, 1.58, [0, 0, 5, 9], s=0.13, gap=0.04)
        _arrow(ax, vx + 1.0, 1.48, vx + 1.0, 1.12)

    # hosts base
    _box(ax, 1.2, 0.5, 7.6, 0.6, DARK, DARK)
    ax.text(5.0, 0.8, "Hosts  (processing elements, PEs)", ha="center", color="white",
            fontsize=10.5, fontweight="bold")

    # orthogonality annotation
    ax.text(9.62, 3.55, "mapping\n(which VM)", ha="center", fontsize=9.5, color=BLUE, rotation=90)
    ax.text(9.62, 2.05, "scheduling\n(order in VM)", ha="center", fontsize=9.5, color=GREEN, rotation=90)
    fig.savefig(os.path.join(OUT, "fig_architecture.pdf"))
    plt.close(fig)


def fig_starvation():
    sched = ["FCFS", "SJF", "Strict", "Aging", "WFQ", "DFPO", "SFQ", "ColaPri"]
    wait = [465.2, 680.4, 721.0, 519.6, 8.2, 50.8, 260.8, 239.5]
    err = [10.9, 22.5, 10.0, 4.3, 1.3, 18.1, 43.1, 46.9]
    colors = [GREY, GREY, AMBER, AMBER, BLUE, BLUE, BLUE, GREEN]
    fig, ax = plt.subplots(figsize=(6.8, 3.4))
    ax.bar(sched, wait, yerr=err, capsize=4, color=colors)
    ax.set_ylabel("max waiting, low class p9 (s)")
    ax.set_title("Starvation under sustained load (lower is better)")
    ax.annotate("-67% vs Strict", xy=(7, 239.5), xytext=(4.9, 430),
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
    sched = ["FCFS", "SJF", "Strict", "WFQ", "DFPO", "SFQ", "ColaPri"]
    p0 = [114.4, 106.7, 63.4, 141.4, 122.0, 105.3, 106.3]
    p5 = [118.3, 105.9, 155.7, 101.4, 108.7, 117.4, 117.0]
    p9 = [124.5, 108.9, 210.6, 61.9, 104.4, 141.3, 139.5]
    x = np.arange(len(sched)); ww = 0.27
    fig, ax = plt.subplots(figsize=(7.0, 3.4))
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
    sfq = [23.9, 124.7, 117.3, 17.2, 26.4]
    cola = [21.0, 127.7, 108.6, 16.6, 21.8]
    strict = [10.0, 71.4, 64.7, 6.2, 10.4]
    x = np.arange(len(ds)); ww = 0.2
    fig, ax = plt.subplots(figsize=(6.6, 3.4))
    ax.bar(x - 1.5 * ww, fcfs, ww, label="FCFS", color=GREY)
    ax.bar(x - 0.5 * ww, sfq, ww, label="SFQ (weighted)", color=BLUE)
    ax.bar(x + 0.5 * ww, cola, ww, label="ColaPri", color=GREEN)
    ax.bar(x + 1.5 * ww, strict, ww, label="Strict (lower bound)", color=AMBER)
    ax.set_xticks(x); ax.set_xticklabels(ds, rotation=15)
    ax.set_ylabel("interactive-class response p0 (s)")
    ax.set_title("Production traces: interactive-class response", fontsize=10)
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


def fig_problem():
    """A single FIFO queue lets one client's bulk submission block an urgent task (head-of-line blocking).
    Two panels: (a) light load served one-by-one; (b) a bulk batch traps the urgent task at the back."""
    URG, BATCH = GREEN, BLUE
    fig, (axA, axB) = plt.subplots(2, 1, figsize=(8.6, 3.4))
    for ax in (axA, axB):
        ax.set_xlim(0, 13); ax.set_ylim(0, 1.5); ax.axis("off")

    def row(ax, n, urgent_idx):
        ax.add_patch(FancyArrowPatch((-0.02, 0.52), (0.28, 0.52), arrowstyle="-|>",
                     mutation_scale=12, color=DARK, lw=1.6))
        for i in range(n):
            fc = URG if i == urgent_idx else BATCH
            ax.add_patch(Rectangle((0.3 + i * 1.0, 0.22), 0.86, 0.6, fc=fc, ec="white", lw=1, zorder=3))
        ax.text(0.3 + n * 1.0 + 0.12, 0.52, "out", fontsize=9, color=DARK, va="center")

    axA.text(0.0, 1.34, "(a) light load: mixed arrivals served one-by-one — the urgent task leaves quickly",
             fontsize=8.6, color=DARK, fontweight="bold")
    row(axA, 5, 1)
    axA.annotate("urgent\nserved soon", xy=(0.3 + 1 * 1.0 + 0.43, 0.84), xytext=(3.0, 1.02),
                 fontsize=7.2, color=GREEN, ha="center",
                 arrowprops=dict(arrowstyle="->", color=GREEN, lw=1.2))
    axA.add_patch(Rectangle((9.7, 1.04), 0.3, 0.22, fc=URG, ec="white"))
    axA.text(10.1, 1.15, "urgent task", fontsize=7.0, va="center", color=DARK)
    axA.add_patch(Rectangle((9.7, 0.70), 0.3, 0.22, fc=BATCH, ec="white"))
    axA.text(10.1, 0.81, "one client's batch", fontsize=7.0, va="center", color=DARK)

    axB.text(0.0, 1.34, "(b) burst: one client submits a bulk batch — the urgent task is stuck at the back",
             fontsize=8.6, color=DARK, fontweight="bold")
    row(axB, 11, 10)
    axB.annotate("urgent task stuck behind one client's\nbulk submission (head-of-line blocking)",
                 xy=(0.3 + 10 * 1.0 + 0.43, 0.84), xytext=(5.7, 1.02),
                 fontsize=7.2, color=RED, ha="center",
                 arrowprops=dict(arrowstyle="->", color=RED, lw=1.3))
    fig.savefig(os.path.join(OUT, "fig_problem.pdf")); plt.close(fig)


def fig_vtime_timeline():
    """Educational, step by step: weighted virtual-time tags tau=k/w_c are stamped once per class, then
    merged by smallest tau; after ten dispatch slots the service split is exactly 6:3:1, low class present."""
    cmap = {0: GREEN, 5: AMBER, 9: GREY}
    wmap = {0: 6, 5: 3, 9: 1}
    tags = []
    for cls in (0, 5, 9):
        for k in range(40):
            tags.append((k / wmap[cls], cls, k))
    tags.sort(key=lambda t: (t[0], t[1]))
    N = 10
    fig, ax = plt.subplots(figsize=(5.0, 4.6))
    ax.set_xlim(0, 6); ax.set_ylim(2.6, 13); ax.axis("off")
    ax.text(3.0, 12.5, r"weighted virtual-time tags  $\tau = k / w_c$", ha="center",
            fontsize=10, color=DARK, fontweight="bold")
    ax.text(3.0, 12.0, r"stamp each task once, then always dispatch the smallest $\tau$",
            ha="center", fontsize=7.6, color=DARK)

    # left: per-class tag streams (first six tags of each class), tau printed inside
    ax.text(1.42, 11.45, "per-class tags", ha="center", fontsize=8, color=DARK, style="italic")
    for col, cls in enumerate((0, 5, 9)):
        x = 0.45 + col * 0.92
        ax.text(x + 0.18, 11.08, f"p{cls} (w={wmap[cls]})", ha="center", fontsize=6.8,
                color=cmap[cls], fontweight="bold")
        for k in range(6):
            y = 10.55 - k * 0.5
            ax.add_patch(Rectangle((x, y), 0.36, 0.42, fc=cmap[cls], ec="white", lw=0.5, zorder=3))
            ax.text(x + 0.18, y + 0.21, f"{k / wmap[cls]:.2f}", ha="center", va="center",
                    fontsize=5.4, color="white")

    ax.annotate("", xy=(4.05, 9.7), xytext=(2.55, 9.7),
                arrowprops=dict(arrowstyle="-|>", color=DARK, lw=1.6))
    ax.text(3.3, 10.15, r"merge by" + "\n" + r"smallest $\tau$", ha="center", fontsize=7.0, color=DARK)

    # right: dispatch order (top = first out), tau and slot index labelled
    ax.text(4.55, 11.45, "dispatch order", ha="center", fontsize=8, color=DARK, style="italic")
    tally = {0: 0, 5: 0, 9: 0}
    for i in range(N):
        tau, cls, k = tags[i]
        tally[cls] += 1
        y = 10.6 - i * 0.62
        ax.add_patch(Rectangle((4.25, y), 0.6, 0.5, fc=cmap[cls], ec="white", lw=0.6, zorder=3))
        ax.text(4.55, y + 0.25, f"{tau:.2f}", ha="center", va="center", fontsize=5.8, color="white")
        ax.text(5.0, y + 0.25, f"#{i + 1}", ha="left", va="center", fontsize=6.2, color=DARK)
    # explanatory note filling the otherwise-empty left-bottom area
    note = ["Why it works:", r"heavier class $\to$ smaller $\tau$ steps,",
            "so it reaches the front more often;", r"every task still gets a finite $\tau$,",
            "so no class is ever starved."]
    for j, ln in enumerate(note):
        ax.text(0.35, 7.15 - j * 0.46, ln, ha="left", fontsize=7.0,
                color=DARK, fontweight="bold" if j == 0 else "normal")
    ax.text(3.0, 4.35, f"after {N} slots:  p0:p5:p9 $=$ {tally[0]}:{tally[5]}:{tally[9]}",
            ha="center", fontsize=8.2, color=DARK, fontweight="bold")

    for c, x in [(0, 0.45), (5, 2.25), (9, 4.05)]:
        ax.add_patch(Rectangle((x, 3.05), 0.3, 0.3, fc=cmap[c], ec="white"))
        ax.text(x + 0.42, 3.2, f"p{c} (w={wmap[c]})", fontsize=6.8, va="center", color=DARK)
    fig.savefig(os.path.join(OUT, "fig_vtime.pdf")); plt.close(fig)


def fig_pareto_starv():
    """Starvation experiment as a trade-off: resp.p0 (x) vs waitMAX.p9 (y); bottom-left is better."""
    name = ["FCFS", "SJF", "Strict", "Aging", "WFQ", "DFPO", "SFQ", "ColaPri"]
    p0 = [248.3, 215.9, 212.0, 238.5, 275.7, 273.2, 260.5, 262.1]
    wm = [465.2, 680.4, 721.0, 519.6, 8.2, 50.8, 260.8, 239.5]
    col = [GREY, GREY, RED, AMBER, BLUE, BLUE, BLUE, GREEN]
    mk = ["o", "o", "s", "s", "^", "^", "D", "*"]
    fig, ax = plt.subplots(figsize=(6.2, 3.8))
    for i in range(len(name)):
        ax.scatter(p0[i], wm[i], s=190 if mk[i] == "*" else 95, c=col[i], marker=mk[i],
                   edgecolors="black", linewidths=0.5, zorder=3)
        off = (-30, -14) if name[i] == "SFQ" else ((7, -12) if name[i] == "DFPO" else (7, 5))
        ax.annotate(name[i], (p0[i], wm[i]), textcoords="offset points", xytext=off, fontsize=8.5)
    ax.set_xlabel("high-class response p0 (s)  —  lower is better")
    ax.set_ylabel("worst-case low-class wait p9 (s)  —  lower is better")
    ax.set_title("Starvation trade-off (sustained load)")
    ax.grid(True, alpha=0.3)
    fig.savefig(os.path.join(OUT, "fig_pareto_starv.pdf")); plt.close(fig)


def fig_adaptive():
    """ColaPri == FCFS under light load; differentiates under heavy load."""
    fig, axes = plt.subplots(1, 2, figsize=(7.6, 3.2), sharey=False)
    for ax, title, f, c in [(axes[0], "light load (no backlog)", [12.6, 12.3], [12.6, 12.3]),
                            (axes[1], "heavy load (burst)", [153.3, 150.2], [93.3, 207.7])]:
        x = np.arange(2); w = 0.36
        ax.bar(x - w / 2, f, w, label="FCFS", color=GREY)
        ax.bar(x + w / 2, c, w, label="ColaPri", color=GREEN)
        ax.set_xticks(x); ax.set_xticklabels(["p0", "p9"]); ax.set_title(title, fontsize=10)
        ax.set_ylabel("mean response (s)")
        ax.legend(fontsize=8)
    fig.suptitle("Adaptive activation: free when idle, differentiates under load", fontsize=10)
    fig.savefig(os.path.join(OUT, "fig_adaptive.pdf")); plt.close(fig)


def fig_datacenter():
    """Per mapping, ColaPri lowers high-class response vs the default scheduler."""
    mp = ["RoundRobin", "MinMin", "MaxMin", "DemandAware"]
    d0 = [41.2, 41.3, 41.4, 41.2]; c0 = [38.4, 38.4, 38.7, 38.4]
    x = np.arange(len(mp)); w = 0.38
    fig, ax = plt.subplots(figsize=(6.6, 3.3))
    ax.bar(x - w / 2, d0, w, label="default  (resp p0)", color=GREY)
    ax.bar(x + w / 2, c0, w, label="ColaPri  (resp p0)", color=GREEN)
    ax.set_xticks(x); ax.set_xticklabels(mp); ax.set_ylabel("high-class response p0 (s)")
    ax.set_ylim(36, 43); ax.set_title("Datacenter: ColaPri speeds p0 under every mapping", fontsize=10)
    ax.legend(fontsize=8)
    fig.savefig(os.path.join(OUT, "fig_datacenter.pdf")); plt.close(fig)


def fig_scalability():
    """Differentiation grows with scale; makespan unchanged; overhead small."""
    n = [200, 500, 1000]
    d0 = [25.3, 64.9, 129.9]; c0 = [24.2, 58.3, 113.3]; c9 = [27.2, 78.0, 165.6]; ov = [14.0, 16.1, 49.3]
    fig, ax1 = plt.subplots(figsize=(6.4, 3.4))
    ax1.plot(n, c0, "o-", color=GREEN, label="ColaPri p0")
    ax1.plot(n, c9, "s-", color=AMBER, label="ColaPri p9")
    ax1.plot(n, d0, "x--", color=GREY, label="default (p0=p9)")
    ax1.set_xlabel("number of tasks"); ax1.set_ylabel("mean response (s)"); ax1.set_xticks(n)
    ax2 = ax1.twinx()
    ax2.plot(n, ov, "^:", color=BLUE, label="ColaPri overhead (ms)")
    ax2.set_ylabel("scheduler overhead (ms)", color=BLUE); ax2.tick_params(axis="y", colors=BLUE)
    ax1.set_title("Scalability: differentiation widens, overhead stays low")
    lines = ax1.get_lines() + ax2.get_lines()
    ax1.legend(lines, [l.get_label() for l in lines], fontsize=8, loc="upper left")
    fig.savefig(os.path.join(OUT, "fig_scalability.pdf")); plt.close(fig)


def fig_sensitivity():
    """Three sensitivity panels: differentiation (p9-p0) vs load, vs PEs, vs threshold."""
    fig, ax = plt.subplots(1, 3, figsize=(9.4, 3.0))
    ax[0].plot([0.5, 1, 2, 3, 5], [116.8, 117.0, 120.1, 115.2, 119.4], "o-", color=GREEN)
    ax[0].set_title("vs offered load"); ax[0].set_xlabel("arrival rate (1/s)")
    ax[1].plot([1, 2, 4, 8], [119.9, 115.2, 109.2, 105.9], "s-", color=AMBER)
    ax[1].set_title("vs VM PEs"); ax[1].set_xlabel("PEs per VM")
    ax[2].plot([0, 5, 15, 30, 60], [115.2, 115.2, 114.4, 112.4, 105.2], "^-", color=BLUE)
    ax[2].set_title("vs adaptive threshold"); ax[2].set_xlabel(r"threshold $\theta$")
    ax[0].set_ylabel("differentiation  p9$-$p0  (s)")
    for a in ax:
        a.grid(True, alpha=0.3)
    fig.suptitle("Parameter sensitivity of ColaPri's differentiation", fontsize=10)
    fig.savefig(os.path.join(OUT, "fig_sensitivity.pdf")); plt.close(fig)


def fig_jain():
    """Weighted Jain fairness per scheduler (main scenario)."""
    name = ["WFQ", "FCFS", "SJF", "DFPO", "SFQ", "ColaPri", "Aging", "Strict"]
    jain = [0.6, 0.7, 0.7, 0.7, 0.8, 0.8, 0.9, 0.9]
    col = [BLUE, GREY, GREY, BLUE, BLUE, GREEN, RED, RED]
    fig, ax = plt.subplots(figsize=(6.2, 3.0))
    ax.bar(name, jain, color=col)
    ax.set_ylabel("weighted Jain index"); ax.set_ylim(0, 1.0)
    ax.set_title("Weighted fairness (higher is better)", fontsize=10)
    fig.savefig(os.path.join(OUT, "fig_jain.pdf")); plt.close(fig)


def fig_traces_speedup():
    """Interactive-class speedup of ColaPri over FCFS per trace."""
    ds = ["PlanetLab", "Alibaba", "Bitbrains", "Materna", "Azure"]
    fcfs = [40.7, 232.0, 185.5, 35.5, 40.9]; cola = [21.0, 127.7, 108.6, 16.6, 21.8]
    sp = [round((1 - c / f) * 100) for c, f in zip(cola, fcfs)]
    fig, ax = plt.subplots(figsize=(6.2, 3.2))
    bars = ax.bar(ds, sp, color=GREEN)
    for b, v in zip(bars, sp):
        ax.text(b.get_x() + b.get_width() / 2, v + 1, f"-{v}%", ha="center", fontsize=9, color=DARK)
    ax.set_ylabel("interactive-class speedup vs FCFS (%)"); ax.set_ylim(0, 60)
    ax.set_xticklabels(ds, rotation=15)
    ax.set_title("ColaPri speeds the interactive class on every trace")
    fig.savefig(os.path.join(OUT, "fig_traces_speedup.pdf")); plt.close(fig)


def fig_algo_flow():
    """Flowchart of Algorithm 1."""
    fig, ax = plt.subplots(figsize=(4.4, 5.4)); ax.set_xlim(0, 6); ax.set_ylim(0, 11); ax.axis("off")

    def cb(y, txt, fc, ec):
        _box(ax, 0.7, y, 3.6, 0.8, fc, ec); ax.text(2.5, y + 0.4, txt, ha="center", va="center", fontsize=8.3, color=DARK)
    cb(9.6, "dispatch event", "#eef3f8", BLUE)
    cb(8.2, r"$L\leftarrow|W|$;  $\alpha\leftarrow$Activation($L$)", "#eef3f8", BLUE)
    ax.add_patch(plt.Polygon([(2.5, 7.55), (4.3, 6.85), (2.5, 6.15), (0.7, 6.85)], closed=True, fc="#fff7e6", ec=AMBER, zorder=2))
    ax.text(2.5, 6.85, r"$\alpha=0$ ?", ha="center", va="center", fontsize=8.5)
    cb(4.7, r"tag untagged: $\tau=k/w^{\mathrm{eff}}$", "#eef6ef", GREEN)
    cb(3.4, r"sort waiting list by $\tau$", "#eef6ef", GREEN)
    cb(2.1, "dispatch to free PEs", "#eef6ef", GREEN)
    _box(ax, 4.7, 6.45, 1.2, 0.8, "#f3eef8", "#9b59b6"); ax.text(5.3, 6.85, "FIFO\n(return)", ha="center", va="center", fontsize=7.8, color=DARK)
    _arrow(ax, 2.5, 9.6, 2.5, 9.0); _arrow(ax, 2.5, 8.2, 2.5, 7.55)
    _arrow(ax, 2.5, 6.15, 2.5, 5.5); ax.text(2.65, 5.55, "no", fontsize=8, color=GREEN)
    _arrow(ax, 4.3, 6.85, 4.7, 6.85); ax.text(4.35, 7.05, "yes", fontsize=8, color="#9b59b6")
    _arrow(ax, 2.5, 4.7, 2.5, 4.2); _arrow(ax, 2.5, 3.4, 2.5, 2.9)
    fig.savefig(os.path.join(OUT, "fig_algo_flow.pdf")); plt.close(fig)


if __name__ == "__main__":
    fig_operation(); fig_architecture(); fig_problem(); fig_vtime_timeline(); fig_algo_flow()
    fig_starvation(); fig_pareto_starv(); fig_weights(); fig_perclass(); fig_adaptive()
    fig_datacenter(); fig_scalability(); fig_sensitivity(); fig_jain()
    fig_traces(); fig_activation()  # fig_traces_speedup retired -> figures/not_used_anymore/ (kept for future)
    print("figures written to", OUT)
