import os

path = 'c:/Users/pc/Desktop/SLIIT/Y2S2/IS/1/src/main/resources/templates/manager/attendance-salary.html'

with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_salary_tab_html = """        <!-- ═══════════ SALARY TAB ═══════════ -->
        <div id="tab-salary" class="tab-panel">
            <!-- Success / error flash banners -->
            <div class="error-banner" th:if="${errorMsg != null and !#strings.isEmpty(errorMsg)}" style="margin-bottom:16px;">
                <i class="fas fa-exclamation-circle"></i>
                <span th:text="${errorMsg}"></span>
            </div>
            <div style="background:#d1fae5;border:1px solid #6ee7b7;border-radius:8px;padding:12px 18px;margin-bottom:16px;color:#065f46;font-size:14px;display:flex;align-items:center;gap:10px;"
                 th:if="${successMsg != null and !#strings.isEmpty(successMsg)}">
                <i class="fas fa-check-circle"></i>
                <span th:text="${successMsg}"></span>
            </div>
            <!-- Month/Year Picker + Generate button -->
            <div class="period-card">
                <h3>Payroll Period</h3>
                <form th:action="@{/manager/salary}" method="get" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;">
                    <div class="form-group" style="margin:0;">
                        <label class="form-label">Month</label>
                        <select name="month" class="form-control">
                            <option th:each="m : ${#numbers.sequence(1,12)}"
                                    th:value="${m}"
                                    th:text="${#temporals.format(#temporals.create(2000,m,1),'MMMM')}"
                                    th:selected="${m == selectedMonth}">Month</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin:0;">
                        <label class="form-label">Year</label>
                        <select name="year" class="form-control">
                            <option th:each="y : ${#numbers.sequence(2023,2030)}"
                                    th:value="${y}" th:text="${y}"
                                    th:selected="${y == selectedYear}">Year</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-outline"><i class="fas fa-search"></i> View</button>
                </form>
                <!-- Generate payroll for the selected month -->
                <form th:action="@{/manager/salary/generate}" method="post" style="margin-top:12px;">
                    <input type="hidden" name="month" th:value="${selectedMonth}">
                    <input type="hidden" name="year"  th:value="${selectedYear}">
                    <button type="submit" class="btn btn-primary">
                        <i class="fas fa-calculator"></i> Generate Payroll
                    </button>
                    <span style="font-size:12px;color:var(--text-muted);margin-left:8px;">
                        Runs the 4-phase engine for all staff — overwrites any existing record for this month.
                    </span>
                </form>
            </div>
            <!-- Totals summary -->
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;" th:if="${payrolls != null and !payrolls.empty}">
                <div class="summary-card">
                    <div>
                        <div class="summary-label">Total Net Salaries (to pay)</div>
                        <div class="summary-value text-green" th:text="'$' + ${#numbers.formatDecimal(totalNetSalary, 1, 2)}">$0.00</div>
                    </div>
                    <div class="summary-icon"><i class="fas fa-hand-holding-usd"></i></div>
                </div>
                <div class="summary-card">
                    <div>
                        <div class="summary-label">Total Employer Cost (net + EPF + ETF)</div>
                        <div class="summary-value" th:text="'$' + ${#numbers.formatDecimal(totalCompanyCost, 1, 2)}">$0.00</div>
                    </div>
                    <div class="summary-icon"><i class="fas fa-building"></i></div>
                </div>
            </div>
            <!-- Payslip breakdown table -->
            <div class="table-card" th:if="${payrolls != null and !payrolls.empty}">
                <div class="table-header-bar">
                    <div class="table-title">
                        Payslip Breakdown
                    </div>
                </div>
                <div style="overflow-x:auto;">
                <table style="min-width:900px;">
                    <thead>
                        <tr>
                            <th>Employee</th>
                            <th>Type</th>
                            <th style="text-align:right;">Base Pay</th>
                            <th style="text-align:right;">Leave Ded.</th>
                            <th style="text-align:right;">OT Pay</th>
                            <th style="text-align:right;">Commission</th>
                            <th style="text-align:right;">Gross</th>
                            <th style="text-align:right;">EPF (8%)</th>
                            <th style="text-align:right;font-weight:700;">Net Salary</th>
                            <th style="text-align:right;color:var(--text-muted);">Co. EPF+ETF</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="p : ${payrolls}">
                            <td>
                                <div style="font-weight:600;" th:text="${p.user.username}">Name</div>
                                <div style="font-size:11px;color:var(--text-muted);" th:text="${p.user.email}">email</div>
                            </td>
                            <td>
                                <span th:if="${p.employmentType == 'PERMANENT'}" class="badge badge-permanent">Permanent</span>
                                <span th:if="${p.employmentType == 'CONTRACT'}"  class="badge badge-contract">Contract</span>
                            </td>
                            <td style="text-align:right;" th:text="'$'+${#numbers.formatDecimal(p.basePay, 1, 2)}">—</td>
                            <td style="text-align:right;color:#ef4444;"
                                th:text="${p.leaveDeduction != null and p.leaveDeduction.compareTo(0) > 0 ? '-$'+#numbers.formatDecimal(p.leaveDeduction,1,2) : '—'}">—</td>
                            <td style="text-align:right;" th:text="'$'+${#numbers.formatDecimal(p.otPay, 1, 2)}">—</td>
                            <td style="text-align:right;" th:text="'$'+${#numbers.formatDecimal(p.commissionPay, 1, 2)}">—</td>
                            <td style="text-align:right;font-weight:600;" th:text="'$'+${#numbers.formatDecimal(p.grossSalary, 1, 2)}">—</td>
                            <td style="text-align:right;color:#ef4444;"
                                th:text="${p.epfEmployee != null and p.epfEmployee.compareTo(0) > 0 ? '-$'+#numbers.formatDecimal(p.epfEmployee,1,2) : '—'}">—</td>
                            <td style="text-align:right;font-weight:700;color:#10b981;" th:text="'$'+${#numbers.formatDecimal(p.netSalary, 1, 2)}">—</td>
                            <td style="text-align:right;color:var(--text-muted);"
                                th:text="'$'+${#numbers.formatDecimal(p.companyEpf.add(p.companyEtf), 1, 2)}">—</td>
                        </tr>
                    </tbody>
                </table>
                </div>
                <div style="font-size:12px;color:var(--text-muted);padding:8px 12px;">
                    * EPF/ETF applicable to PERMANENT workers only &nbsp;|&nbsp;
                    OT = hours beyond 8h/day &times; OT hourly rate &nbsp;|&nbsp;
                    Commission = % of FINALIZED sales in period
                </div>
            </div>
            <div th:if="${payrolls == null or payrolls.empty}"
                 style="color:var(--text-muted);font-size:14px;padding:24px 0;">
                No payroll generated yet for this period. Click <strong>Generate Payroll</strong> above.
            </div>
            <!-- Per-Worker Rate Settings -->
            <div class="table-card" th:if="${staff != null and !staff.empty}" style="margin-top:24px;">
                <div class="table-header-bar">
                    <div class="table-title">Worker Rate Settings</div>
                    <span style="font-size:12px;color:var(--text-muted);">Update rates, then regenerate payroll</span>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Employee</th>
                            <th>Type</th>
                            <th>Basic Salary / Daily Wage</th>
                            <th>OT Rate /hr</th>
                            <th>Commission %</th>
                            <th>Save</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="emp : ${staff}">
                            <td>
                                <div style="font-weight:600;" th:text="${emp.username}">Name</div>
                                <div style="font-size:11px;color:var(--text-muted);" th:text="${emp.role}">Role</div>
                            </td>
                            <td>
                                <span th:if="${emp.contractType == 'PERMANENT'}" class="badge badge-permanent">Permanent</span>
                                <span th:if="${emp.contractType == 'CONTRACT'}"  class="badge badge-contract">Contract</span>
                                <span th:if="${emp.contractType == null}"        class="badge badge-permanent">Permanent</span>
                            </td>
                            <td colspan="3">
                                <form th:action="@{'/manager/salary/update/' + ${emp.id}}" method="post"
                                      style="display:flex;gap:6px;align-items:center;flex-wrap:wrap;">
                                    <input type="hidden" name="redirectMonth" th:value="${selectedMonth}">
                                    <input type="hidden" name="redirectYear"  th:value="${selectedYear}">
                                    <select name="contractType" class="form-control btn-xs" style="width:110px;padding:4px 6px;">
                                        <option value="PERMANENT" th:selected="${emp.contractType == 'PERMANENT' or emp.contractType == null}">Permanent</option>
                                        <option value="CONTRACT"  th:selected="${emp.contractType == 'CONTRACT'}">Contract</option>
                                    </select>
                                    <input type="number" name="salaryRate" step="0.01" min="0" class="form-control btn-xs" style="width:90px;padding:4px 6px;" th:value="${emp.salaryRate}" placeholder="Basic $">
                                    <input type="number" name="dailyWage" step="0.01" min="0" class="form-control btn-xs" style="width:80px;padding:4px 6px;" th:value="${emp.dailyWage}" placeholder="Daily $">
                            </td>
                            <td>
                                    <input type="number" name="otHourlyRate" step="0.01" min="0" class="form-control btn-xs" style="width:75px;padding:4px 6px;" th:value="${emp.otHourlyRate}" placeholder="$/hr">
                            </td>
                            <td>
                                    <input type="number" name="commissionRate" step="0.01" min="0" max="100" class="form-control btn-xs" style="width:65px;padding:4px 6px;" th:value="${emp.commissionRate}" placeholder="%">
                            </td>
                            <td>
                                    <button type="submit" class="btn btn-primary btn-xs"><i class="fas fa-save"></i></button>
                                </form>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <!-- end salary tab -->\n"""

start_idx = -1
end_idx = -1
for i, line in enumerate(lines):
    if "SALARY TAB" in line:
        start_idx = i
    if "end salary tab" in line:
        end_idx = i

if start_idx != -1 and end_idx != -1:
    new_lines = lines[:start_idx] + [new_salary_tab_html] + lines[end_idx+1:]
    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    print("Replace successful")
else:
    print(f"Could not find markers. start={start_idx}, end={end_idx}")
